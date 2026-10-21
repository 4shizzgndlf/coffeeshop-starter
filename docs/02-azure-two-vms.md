# Deploy to Azure, part 2: the app and the database on separate VMs

In [part 1](01-azure-one-vm.md) everything ran on one VM. Now you split it in two:

- **`vm-app`** runs Caddy, the frontend and Spring Boot. It has a public IP address.
- **`vm-db`** runs only MySQL. It has **no public IP address**: it can't be reached from the
  internet at all.

Both VMs are in the same **virtual network** (VNet), so they talk to each other using private IP
addresses. A firewall rule lets only the app's subnet connect to MySQL.

```text
                      Internet
                         │  nsg-app: 80 and 443 from anywhere, 22 only from your IP
                         ▼
┌─ vnet-coffeeshop  10.0.0.0/16 ───────────────────────────────────────────────┐
│                                                                              │
│   snet-app  10.0.1.0/24                     snet-db  10.0.2.0/24             │
│  ┌──────────────────────────────┐          ┌──────────────────────────────┐  │
│  │ vm-app                       │          │ vm-db                        │  │
│  │ public IP + DNS name         │   3306   │ 10.0.2.10, no public IP      │  │
│  │ caddy, frontend, api ────────┼─────────▶│ db (MySQL)                   │  │
│  └──────────────────────────────┘          └──────────────────────────────┘  │
│                                     nsg-db: 3306 and 22 only from snet-app   │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Why split them?**

- **The database is off the internet.** With no public IP, there is no address to attack.
  The only way in is through `vm-app`.
- **Each part can change on its own.** You can replace or resize `vm-app`, or deploy a new
  version, without touching the VM that holds the data.
- **It's the first step towards bigger setups:** several app VMs behind a load balancer, or a
  managed database such as *Azure Database for MySQL*. Both need the database on the private
  network, as here.

**Time:** about 40 minutes.

## Contents

1. [Before you start](#1-before-you-start)
2. [Choose names](#2-choose-names)
3. [Create the resource group](#3-create-the-resource-group)
4. [Create the two firewalls](#4-create-the-two-firewalls)
5. [Create the network and the subnets](#5-create-the-network-and-the-subnets)
6. [Create the database VM](#6-create-the-database-vm)
7. [Create the app VM](#7-create-the-app-vm)
8. [Wait for Docker on both VMs](#8-wait-for-docker-on-both-vms)
9. [Create the two .env files](#9-create-the-two-env-files)
10. [Start MySQL on vm-db](#10-start-mysql-on-vm-db)
11. [Check the private connection](#11-check-the-private-connection)
12. [Start the app on vm-app](#12-start-the-app-on-vm-app)
13. [HTTPS, new versions and cleaning up](#13-https-new-versions-and-cleaning-up)
14. [Troubleshooting](#troubleshooting)

## New words

| Word | Meaning |
|------|---------|
| **Virtual network (VNet)** | Your own private network in Azure, with an address range like `10.0.0.0/16` (`10.0.x.x`). VMs in the same VNet can reach each other |
| **Subnet** | A part of the VNet's address range, e.g. `10.0.1.0/24` (`10.0.1.x`). Each subnet can have its own NSG |
| **Private IP address** | An address inside the VNet, like `10.0.2.10`. It only works inside the VNet, not from the internet |
| **NSG on a subnet** | In part 1 the NSG was attached to the VM. Here it is attached to the subnet, so it protects every VM in that subnet |
| **Jump host** | `vm-db` has no public IP, so you can't SSH to it directly. You SSH to `vm-app` and *jump* from there to `vm-db`'s private IP |

## The files used

| File | Where it runs | What it does |
|------|---------------|--------------|
| `deploy/cloud-init.yaml` | On both VMs, once, when they are created | Adds 2 GB of swap and installs Docker (the same file as in part 1) |
| `deploy/two-vms/db/compose.yaml` | `vm-db` | Only MySQL, with port 3306 published on the VM's private IP |
| `deploy/two-vms/app/compose.yaml` | `vm-app` | Caddy, the frontend and the api, with the images from GHCR. No `db` service: the api connects to `DB_HOST` |
| `deploy/two-vms/app/Caddyfile` | `vm-app` | The same as in part 1 |

Compared to part 1, the only change for Spring Boot is one environment variable in
`deploy/two-vms/app/compose.yaml`:

```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://${DB_HOST}:3306/coffeeshop
```

It replaces `spring.datasource.url` from `application-docker.properties` (`jdbc:mysql://db:3306/...`),
because there is no `db` container on `vm-app`. Spring Boot lets an environment variable override
any property: `SPRING_DATASOURCE_URL` is `spring.datasource.url` in upper case, with `_` instead of `.`.

## 1. Before you start

You need the same things as in [part 1, step 1](01-azure-one-vm.md#1-before-you-start):

- The Azure CLI, logged in.
- A release on GHCR (e.g. `v1.0.0`) with **public** images.
- A bash or zsh terminal in the root of the repository.

If part 1's resource group still exists, delete it first (`az group delete --name rg-coffeeshop
--yes`), or choose a different `DNS_LABEL` below: a DNS name can only be used once per region.

**What it costs:** two `Standard_B1ms` VMs, so about 0.045 USD an hour, or around 32 USD a month
if they run all the time, plus the disks and one public IP.

## 2. Choose names

In zsh, run `setopt interactivecomments` first (see [part 1](01-azure-one-vm.md#1-before-you-start)).
Change `DNS_LABEL` and `IMAGE_PREFIX`, then run:

```bash
RG=rg-coffeeshop-vnet
LOCATION=swedencentral
SIZE=Standard_B1ms
VNET=vnet-coffeeshop
APP_VM=vm-app
DB_VM=vm-db
DB_IP=10.0.2.10
DNS_LABEL=coffeeshop-ab12
IMAGE_PREFIX=ghcr.io/your-github-user/coffeeshop
MY_IP=$(curl -s https://api.ipify.org)
echo "My IP is $MY_IP"
```

| Variable | Meaning |
|----------|---------|
| `LOCATION` | An allowed region with `Standard_B1ms`, see [part 1, step 2](01-azure-one-vm.md#2-choose-names-and-a-region) |
| `SIZE` | 1 vCPU and 2 GB of memory, for both VMs |
| `DB_IP` | `vm-db`'s fixed private IP address, inside `snet-db` (`10.0.2.0/24`) |
| `DNS_LABEL` | `vm-app`'s DNS name. Must be unique in the region |
| `IMAGE_PREFIX` | `ghcr.io/<owner>/<repository>`, in lower case |

## 3. Create the resource group

```bash
az group create --name $RG --location $LOCATION -o table
```

## 4. Create the two firewalls

Each subnet gets its own NSG.

**`nsg-app`** is the same as in part 1: SSH from your IP, HTTP and HTTPS from anywhere.

```bash
az network nsg create --resource-group $RG --name nsg-app -o none

az network nsg rule create --resource-group $RG --nsg-name nsg-app \
  --name Allow-SSH-from-my-IP --priority 100 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes $MY_IP --destination-port-ranges 22 \
  -o none

az network nsg rule create --resource-group $RG --nsg-name nsg-app \
  --name Allow-HTTP-HTTPS --priority 110 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes Internet --destination-port-ranges 80 443 \
  -o none
```

**`nsg-db`** only lets the app subnet (`10.0.1.0/24`) in, on two ports: 3306 for MySQL, and 22 so
you can jump to `vm-db` from `vm-app`.

```bash
az network nsg create --resource-group $RG --name nsg-db -o none

az network nsg rule create --resource-group $RG --nsg-name nsg-db \
  --name Allow-MySQL-from-app --priority 100 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes 10.0.1.0/24 --destination-port-ranges 3306 \
  -o none

az network nsg rule create --resource-group $RG --nsg-name nsg-db \
  --name Allow-SSH-from-app --priority 110 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes 10.0.1.0/24 --destination-port-ranges 22 \
  -o none

az network nsg rule create --resource-group $RG --nsg-name nsg-db \
  --name Deny-rest-of-VNet --priority 4000 \
  --direction Inbound --access Deny --protocol '*' \
  --source-address-prefixes VirtualNetwork --destination-port-ranges '*' \
  -o none
```

**Why the deny rule?** Every NSG has built-in rules, with priorities from 65000 up. Show them with:

```bash
az network nsg rule list --resource-group $RG --nsg-name nsg-db --include-default -o table
```

One of them, `AllowVnetInBound` (65000), allows **everything** from inside the VNet. Without
`Deny-rest-of-VNet`, any VM in any subnet could reach any port on `vm-db`. With it, Azure checks
the rules from the lowest number up:

| Priority | Rule | Traffic to vm-db |
|----------|------|------------------|
| 100 | `Allow-MySQL-from-app` | From `snet-app` to port 3306: **allowed** |
| 110 | `Allow-SSH-from-app` | From `snet-app` to port 22: **allowed** |
| 4000 | `Deny-rest-of-VNet` | Anything else from inside the VNet: **blocked** |
| 65000 | `AllowVnetInBound` (built in) | Never reached for VNet traffic: rule 4000 has already matched |
| 65500 | `DenyAllInBound` (built in) | Everything from outside the VNet: **blocked** (and `vm-db` has no public IP anyway) |

The `'*'` are in quotes so your shell doesn't treat them as wildcards for file names.

## 5. Create the network and the subnets

```bash
az network vnet create --resource-group $RG --name $VNET \
  --address-prefixes 10.0.0.0/16 -o none

az network vnet subnet create --resource-group $RG --vnet-name $VNET \
  --name snet-app --address-prefixes 10.0.1.0/24 \
  --network-security-group nsg-app -o none

az network vnet subnet create --resource-group $RG --vnet-name $VNET \
  --name snet-db --address-prefixes 10.0.2.0/24 \
  --network-security-group nsg-db \
  --default-outbound true -o none

az network vnet subnet list --resource-group $RG --vnet-name $VNET \
  --query "[].{name:name, prefix:addressPrefix, nsg:networkSecurityGroup.id, outbound:defaultOutboundAccess}" -o table
```

**`--default-outbound true`:** subnets in new VNets are *private* by default. A VM in a private
subnet without a public IP can't reach the internet, so `vm-db` couldn't download Docker or the
MySQL image. This option gives `snet-db` outbound access. It only works one way: `vm-db` can open
connections *to* the internet, but nothing on the internet can connect *to* `vm-db`. Check that
the `Outbound` column says `True` for `snet-db`.

`vm-app` doesn't need this: it reaches the internet through its own public IP.

In production you would use a *NAT gateway* for outbound traffic instead. It does the same job
with a fixed address you control, but it costs extra.

## 6. Create the database VM

```bash
az vm create \
  --resource-group $RG \
  --name $DB_VM \
  --image Ubuntu2404 \
  --size $SIZE \
  --admin-username azureuser \
  --generate-ssh-keys \
  --vnet-name $VNET \
  --subnet snet-db \
  --private-ip-address $DB_IP \
  --public-ip-address "" \
  --nsg "" \
  --custom-data deploy/cloud-init.yaml \
  -o table
```

| Option | Meaning |
|--------|---------|
| `--vnet-name`, `--subnet snet-db` | Put the VM in the database subnet |
| `--private-ip-address $DB_IP` | A fixed private IP. Otherwise Azure picks one, and it's this address that goes in the app's `.env` |
| `--public-ip-address ""` | **No public IP.** `""` means "none" |
| `--nsg ""` | No NSG on the VM itself: `nsg-db` on the subnet already protects it |

Azure keeps the first four addresses of every subnet (`.0` to `.3`), so `.4` is the lowest you can use.

## 7. Create the app VM

```bash
az vm create \
  --resource-group $RG \
  --name $APP_VM \
  --image Ubuntu2404 \
  --size $SIZE \
  --admin-username azureuser \
  --generate-ssh-keys \
  --vnet-name $VNET \
  --subnet snet-app \
  --nsg "" \
  --public-ip-sku Standard \
  --public-ip-address-dns-name $DNS_LABEL \
  --custom-data deploy/cloud-init.yaml \
  -o table

APP_FQDN=$(az vm show --show-details --resource-group $RG --name $APP_VM --query fqdns -o tsv)
echo "$APP_FQDN"
```

List the IP addresses of both VMs. `vm-db` only has a private one:

```bash
az vm list-ip-addresses --resource-group $RG -o table
```

## 8. Wait for Docker on both VMs

`vm-app` is reached as in part 1:

```bash
ssh azureuser@$APP_FQDN cloud-init status --wait
```

`vm-db` is reached by **jumping** through `vm-app` with `-J`:

```bash
ssh -J azureuser@$APP_FQDN azureuser@$DB_IP cloud-init status --wait
```

`-J` (jump) makes SSH connect to `vm-app` first, and from there on to `10.0.2.10`. Your private
key never leaves your machine: the connection to `vm-db` is tunnelled through `vm-app`. SSH asks
you to trust each server's key the first time: type `yes`.

Both commands should end with `status: done`. If the one for `vm-db` says `status: error`, see
[Troubleshooting](#troubleshooting).

## 9. Create the two .env files

Both VMs need the same database password: `vm-db` creates the MySQL user with it, and `vm-app`
logs in with it. So you make the password once, on your machine:

```bash
DB_PASSWORD=$(openssl rand -hex 24)

cat > deploy/two-vms/db/.env <<EOF
DB_USERNAME=coffeeshop
DB_PASSWORD=$DB_PASSWORD
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 24)
EOF

cat > deploy/two-vms/app/.env <<EOF
IMAGE_PREFIX=$IMAGE_PREFIX
APP_VERSION=1.0.0
SITE_ADDRESS=:80
DB_HOST=$DB_IP
DB_USERNAME=coffeeshop
DB_PASSWORD=$DB_PASSWORD
EOF
```

Notice that `vm-app` never gets the MySQL **root** password: it only needs the `coffeeshop` user.

## 10. Start MySQL on vm-db

Copy the folder (it becomes `~/coffeeshop-db` on `vm-db`) and start MySQL. `--wait` waits until
MySQL is healthy:

```bash
scp -J azureuser@$APP_FQDN -r deploy/two-vms/db azureuser@$DB_IP:coffeeshop-db

ssh -J azureuser@$APP_FQDN azureuser@$DB_IP "cd coffeeshop-db && docker compose up -d --wait && docker compose ps"
```

The `PORTS` column shows `0.0.0.0:3306->3306/tcp`: MySQL is published on every network card of
`vm-db`. That is safe here because `vm-db` only has a private network card, and `nsg-db` only lets
`snet-app` in.

## 11. Check the private connection

Before starting the app, check that `vm-app` can reach MySQL. `nc -z` only tests if a TCP
connection can be opened:

```bash
ssh azureuser@$APP_FQDN "nc -zv -w 3 $DB_IP 3306"
```

```text
Connection to 10.0.2.10 3306 port [tcp/mysql] succeeded!
```

**Try it: see the NSG at work.** Try a port that `nsg-db` doesn't allow:

```bash
ssh azureuser@$APP_FQDN "nc -zv -w 3 $DB_IP 8080"
```

It waits 3 seconds and gives up (`timed out`), because `Deny-rest-of-VNet` silently drops the
packets. Nothing listens on port 8080 on `vm-db`, so without that rule you would get `Connection
refused` immediately instead. A *refused* connection means "the packet arrived, but nothing is
listening". A *timeout* usually means "a firewall dropped it".

## 12. Start the app on vm-app

```bash
scp -r deploy/two-vms/app azureuser@$APP_FQDN:coffeeshop

ssh azureuser@$APP_FQDN "cd coffeeshop && docker compose pull && docker compose up -d && docker compose ps"

echo "http://$APP_FQDN"
```

Open the address in your browser, put something in the cart and place an order.

**Where is the order?** Not on `vm-app`. Log in to `vm-db` and look:

```bash
ssh -J azureuser@$APP_FQDN azureuser@$DB_IP
```

```bash
# On vm-db
cd coffeeshop-db
docker compose exec db sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" coffeeshop -e "select id, status, total from orders"'
exit
```

The single quotes make sure `$MYSQL_USER` and `$MYSQL_PASSWORD` are read *inside* the MySQL
container, where Compose has set them from `.env`. The password never appears in your terminal.

## 13. HTTPS, new versions and cleaning up

These work as in part 1, but everything happens on `vm-app`. `vm-db` doesn't change when you
deploy a new version.

**HTTPS** ([part 1, step 9](01-azure-one-vm.md#9-https-with-a-free-certificate)):

```bash
ssh azureuser@$APP_FQDN "cd coffeeshop && sed -i 's/^SITE_ADDRESS=.*/SITE_ADDRESS=$APP_FQDN/' .env && docker compose up -d"
```

Then open `https://$APP_FQDN`.

**A new version** ([part 1, step 10](01-azure-one-vm.md#10-deploy-a-new-version)):

```bash
ssh azureuser@$APP_FQDN "cd coffeeshop && sed -i 's/^APP_VERSION=.*/APP_VERSION=1.1.0/' .env && docker compose pull && docker compose up -d"
```

**Stop paying.** Stop both VMs:

```bash
az vm deallocate --resource-group $RG --name $APP_VM
az vm deallocate --resource-group $RG --name $DB_VM
```

Start them again, the database first:

```bash
az vm start --resource-group $RG --name $DB_VM
az vm start --resource-group $RG --name $APP_VM
```

If `vm-app` is ready before MySQL is, the api fails to connect and Docker restarts it
(`restart: unless-stopped`) until it works.

Or delete everything, **including the data**:

```bash
az group delete --name $RG --yes --no-wait
```

## Troubleshooting

Most problems are the same as in [part 1](01-azure-one-vm.md#troubleshooting). These are new:

| Problem | Fix |
|---------|-----|
| `cloud-init status` on `vm-db` says `error`, and `/var/log/cloud-init-output.log` shows `Could not resolve 'download.docker.com'` | `vm-db` has no internet access: `snet-db` was created without `--default-outbound true`. Check with `az network vnet subnet show -g $RG --vnet-name $VNET -n snet-db --query defaultOutboundAccess`. The simplest fix is to delete the resource group and start again |
| `ssh -J`: `Connection timed out` at `10.0.2.10` | `nsg-db` must allow port 22 from `10.0.1.0/24`. Check with `az network nsg rule list -g $RG --nsg-name nsg-db -o table` |
| `ssh`: `REMOTE HOST IDENTIFICATION HAS CHANGED` | You created a new VM with the same name or IP as before, so its key is new. Remove the old key: `ssh-keygen -R $APP_FQDN` or `ssh-keygen -R $DB_IP` |
| `api` keeps restarting, log says `Communications link failure` | `vm-app` can't reach MySQL. Is it running on `vm-db` (`docker compose ps`)? Does `nc -zv $DB_IP 3306` work from `vm-app` (step 11)? Is `DB_HOST` in `vm-app`'s `.env` correct? |
| `api` keeps restarting, log says `Access denied for user 'coffeeshop'` | `DB_PASSWORD` differs between the two `.env` files, or MySQL on `vm-db` was first started with another password. MySQL keeps that password in its volume; `docker compose down -v` on `vm-db` deletes it, **with all the data** |
| `az vm create` for `vm-db`: `PrivateIPAddressInUse` | Another NIC already has `10.0.2.10`, e.g. from a deleted VM whose network card is still there. Choose another `DB_IP` in `10.0.2.4` – `10.0.2.254`, and use it in `.env` too |

## Going further

- **A managed database.** *Azure Database for MySQL – Flexible Server* with *private access*
  replaces `vm-db`: Azure runs MySQL, installs updates and takes backups. It goes in a subnet of
  your VNet, just like `vm-db`, and the app finds it the same way, through `DB_HOST`.
- **Azure Bastion** replaces the jump through `vm-app`: you get SSH to VMs without public IPs
  from the Azure portal, without opening port 22 on any VM.
- **More app VMs.** Because the data is no longer on `vm-app`, you can run two or more of them
  behind an *Azure Load Balancer*, all using the same `vm-db`.
- **Continuous deployment.** A GitHub Actions job could run the `ssh ... docker compose pull &&
  docker compose up -d` command from step 13 after every release, with the SSH key stored as a
  repository secret.
