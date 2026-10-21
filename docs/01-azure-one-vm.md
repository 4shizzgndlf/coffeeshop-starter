# Deploy to Azure, part 1: everything on one VM

In this guide you put the whole Coffee Shop on one virtual machine in Azure, using only the
Azure CLI (`az`) and SSH. MySQL, Spring Boot, Nginx and Caddy run in Docker on the VM, just like
`compose.full.yaml` on your own machine. The difference is that nothing is built on the server:
it downloads the images that GitHub Actions published to GHCR.

```text
 browser ──▶ http://coffeeshop-ab12.swedencentral.cloudapp.azure.com
                │
                │  nsg-coffeeshop:  80 and 443 from anywhere, 22 only from your IP
                ▼
 ┌─ vm-coffeeshop ── Ubuntu 24.04, Standard_B1ms (1 vCPU, 2 GB) ────────────────┐
 │  Docker Compose (deploy/one-vm/compose.yaml)                                 │
 │                                                                              │
 │   caddy :80/:443 ──/api/*──▶ api (Spring Boot) ──▶ db (MySQL, no ports)      │
 │                  └─ else ──▶ frontend (Nginx)                                │
 └──────────────────────────────────────────────────────────────────────────────┘
          ▲ docker compose pull
 ghcr.io/<owner>/coffeeshop-api:1.0.0, ghcr.io/<owner>/coffeeshop-frontend:1.0.0
```

The next guide, [part 2](02-azure-two-vms.md), moves the database to a VM of its own in a
private network.

**Time:** about 30 minutes, most of it waiting for Azure and Docker.

## Contents

1. [Before you start](#1-before-you-start)
2. [Choose names and a region](#2-choose-names-and-a-region)
3. [Create the resource group](#3-create-the-resource-group)
4. [Create a firewall: the network security group](#4-create-a-firewall-the-network-security-group)
5. [Create the VM](#5-create-the-vm)
6. [Wait for Docker](#6-wait-for-docker)
7. [Copy the configuration to the VM](#7-copy-the-configuration-to-the-vm)
8. [Start the application](#8-start-the-application)
9. [HTTPS with a free certificate](#9-https-with-a-free-certificate)
10. [Deploy a new version](#10-deploy-a-new-version)
11. [Stop paying](#11-stop-paying)
12. [Troubleshooting](#troubleshooting)

## The files used

| File | Where it runs | What it does |
|------|---------------|--------------|
| `deploy/cloud-init.yaml` | On the VM, once, when it is created | Adds 2 GB of swap and installs Docker |
| `deploy/one-vm/compose.yaml` | On the VM | The four containers, with the images from GHCR |
| `deploy/one-vm/Caddyfile` | On the VM | Like the `Caddyfile` in the repository root, but the address comes from `.env`, so it can do HTTPS |
| `deploy/one-vm/.env.example` | Your machine, then the VM | The template for `.env`: image names, version and passwords |

## 1. Before you start

**Azure CLI.** Install it and log in:

```bash
az version
az login
az account show -o table
```

`az account show` should show your *Azure for Students* subscription.

**A release with images on GHCR.** The VM runs the images that `.github/workflows/release.yml`
publishes (see [CI and releases](../README.md#ci-and-releases) in the README):

1. Push the repository to GitHub.
2. Tag a version and push the tag:

   ```bash
   git tag -a v1.0.0 -m "First release"
   git push origin v1.0.0
   ```

3. Wait until the *Release* workflow is green (the *Actions* tab on GitHub, or `gh run watch`).
4. **Make both images public.** New packages on GHCR are private, and then the VM can't download
   them. On GitHub, go to your profile → *Packages* → `coffeeshop-api` → *Package settings* →
   *Change visibility* → *Public*. Do the same for `coffeeshop-frontend`.

   Check it from your own machine. This must work **without** being logged in to GHCR:

   ```bash
   docker logout ghcr.io
   docker manifest inspect ghcr.io/<owner>/coffeeshop-api:1.0.0 > /dev/null && echo "public"
   ```

   Want to keep them private? See [Private images](#private-images) at the end.

**A terminal.** All commands are for bash or zsh: the Terminal on macOS and Linux, or Git Bash or
WSL on Windows. Run them **from the root of the repository**, because some of them read files
from `deploy/`. The variables you set in step 2 only exist in that terminal window: keep it open,
or set them again in a new one.

In **zsh** (the default on macOS), run this once in the window first. Otherwise zsh treats the
`# comment` lines in the commands as commands:

```bash
setopt interactivecomments
```

**What it costs.** A `Standard_B1ms` VM costs about 0.02 USD an hour in Sweden Central, which is
roughly 16 USD a month if it runs all the time. The public IP adds about 0.005 USD an hour and the
disk a few dollars a month. Azure for Students gives you 100 USD of credit. [Stop or delete](#11-stop-paying)
the VM when you don't need it.

## 2. Choose names and a region

Change `DNS_LABEL` and `IMAGE_PREFIX`, then run:

```bash
RG=rg-coffeeshop
LOCATION=swedencentral
VM=vm-coffeeshop
NSG=nsg-coffeeshop
SIZE=Standard_B1ms
DNS_LABEL=coffeeshop-ab12
IMAGE_PREFIX=ghcr.io/your-github-user/coffeeshop
MY_IP=$(curl -s https://api.ipify.org)
echo "My IP is $MY_IP"
```

| Variable | Meaning |
|----------|---------|
| `RG` | The resource group: a folder for everything in this guide |
| `LOCATION` | The Azure region |
| `VM` | The virtual machine |
| `NSG` | Its firewall |
| `SIZE` | `Standard_B1ms`: 1 vCPU and 2 GB of memory |
| `DNS_LABEL` | Becomes `coffeeshop-ab12.swedencentral.cloudapp.azure.com`. **Must be unique in the region**: use your initials and a number. Lower case letters, digits and `-` |
| `IMAGE_PREFIX` | Your images without `-api`/`-frontend`, in lower case: `ghcr.io/<owner>/<repository>` |
| `MY_IP` | Your public IP address, for the SSH rule. [api.ipify.org](https://api.ipify.org) answers with the address it sees you coming from |

**Is the region allowed?** Azure for Students only lets you create resources in a few regions.
List them, and pick one of them as `LOCATION`:

```bash
az policy assignment list \
  --query "[?displayName=='Allowed resource deployment regions'].parameters.listOfAllowedLocations.value[]" \
  -o tsv
```

If it prints nothing, your subscription has no such limit.

**Is the VM size available there?** Not every size is available everywhere, and some are blocked
for student subscriptions. This takes about a minute:

```bash
az vm list-skus --location $LOCATION --size $SIZE --all \
  --query "[?name=='$SIZE'].{size:name, restrictions:join(', ', restrictions[].reasonCode)}" \
  -o table
```

An empty *Restrictions* column means you can use it. `NotAvailableForSubscription` means: pick
another region from the list. (When this guide was written, `Standard_B1ms` was available in
`swedencentral` but not in `germanywestcentral`.)

## 3. Create the resource group

A resource group holds everything you create in this guide. At the end you delete the group, and
everything inside it goes with it.

```bash
az group create --name $RG --location $LOCATION -o table
```

## 4. Create a firewall: the network security group

A **network security group** (NSG) is Azure's firewall in front of the VM. It has rules that let
traffic in. Everything else is blocked. You create it **before** the VM, so the VM is never
reachable on a port you didn't choose.

```bash
az network nsg create --resource-group $RG --name $NSG -o none

# SSH (port 22) only from your own IP address
az network nsg rule create --resource-group $RG --nsg-name $NSG \
  --name Allow-SSH-from-my-IP --priority 100 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes $MY_IP --destination-port-ranges 22 \
  -o none

# HTTP and HTTPS (ports 80 and 443) from anywhere on the internet
az network nsg rule create --resource-group $RG --nsg-name $NSG \
  --name Allow-HTTP-HTTPS --priority 110 \
  --direction Inbound --access Allow --protocol Tcp \
  --source-address-prefixes Internet --destination-port-ranges 80 443 \
  -o none

az network nsg rule list --resource-group $RG --nsg-name $NSG -o table
```

- The **priority** decides the order: Azure checks rules from the lowest number up, and the first
  rule that matches wins.
- `Internet` is a *service tag*: every address outside Azure's own network.
- Port 3306 (MySQL) is **not** opened. MySQL isn't even published by Docker: only the `api`
  container can reach it, through Docker's own network on the VM.

## 5. Create the VM

```bash
az vm create \
  --resource-group $RG \
  --name $VM \
  --image Ubuntu2404 \
  --size $SIZE \
  --admin-username azureuser \
  --generate-ssh-keys \
  --nsg $NSG \
  --public-ip-sku Standard \
  --public-ip-address-dns-name $DNS_LABEL \
  --custom-data deploy/cloud-init.yaml \
  -o table
```

| Option | Meaning |
|--------|---------|
| `--image Ubuntu2404` | Ubuntu Server 24.04 LTS |
| `--size $SIZE` | `Standard_B1ms`: 1 vCPU and 2 GB of memory. Enough for MySQL and Spring Boot together, with the swap file as a safety net |
| `--admin-username azureuser` | The Linux user you log in as. `cloud-init.yaml` gives this user access to Docker |
| `--generate-ssh-keys` | Uses your key in `~/.ssh/id_rsa.pub`, or creates one if you don't have it. There is no password |
| `--nsg $NSG` | Use the firewall from step 4 |
| `--public-ip-sku Standard` | A static public IP address: it stays the same when you stop and start the VM |
| `--public-ip-address-dns-name` | A DNS name for that IP, so you don't have to type the IP. HTTPS needs it too |
| `--custom-data deploy/cloud-init.yaml` | A script that runs once at the first start: it installs Docker |

When it finishes (a minute or two), it prints the VM's `PublicIpAddress` and `Fqdns`. Save the
DNS name in a variable:

```bash
FQDN=$(az vm show --show-details --resource-group $RG --name $VM --query fqdns -o tsv)
echo "$FQDN"
```

## 6. Wait for Docker

The VM is running, but cloud-init may still be installing Docker. Log in and wait for it:

```bash
ssh azureuser@$FQDN cloud-init status --wait
```

- The first time, SSH asks if you trust the server's key (*Are you sure you want to continue
  connecting?*). Type `yes`.
- Dots appear while it works. After a few minutes it prints `status: done`.

Check the result:

```bash
ssh azureuser@$FQDN
```

```bash
# Now you're on the VM
docker --version
docker compose version
free -h                  # "Swap: 2.0Gi" comes from cloud-init.yaml
exit
```

If `docker` says *permission denied*, you logged in before cloud-init had finished: `exit` and
log in again.

## 7. Copy the configuration to the VM

The VM needs three files: `compose.yaml`, the `Caddyfile` and a `.env` file with your settings.
You create `.env` on your own machine with new, random passwords:

```bash
cat > deploy/one-vm/.env <<EOF
IMAGE_PREFIX=$IMAGE_PREFIX
APP_VERSION=1.0.0
SITE_ADDRESS=:80
DB_USERNAME=coffeeshop
DB_PASSWORD=$(openssl rand -hex 24)
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 24)
EOF

cat deploy/one-vm/.env
```

- `.env` is in `.gitignore`. **Never commit it**: it contains the database passwords.
- `APP_VERSION` is the Git tag without the `v`: tag `v1.0.0` gives the image `:1.0.0`.
- `SITE_ADDRESS=:80` means plain HTTP for now. Step 9 changes it to HTTPS.

Copy the folder to the VM. It becomes `~/coffeeshop` there:

```bash
scp -r deploy/one-vm azureuser@$FQDN:coffeeshop
```

## 8. Start the application

```bash
ssh azureuser@$FQDN
```

```bash
# On the VM
cd coffeeshop
docker compose pull       # download the images from GHCR
docker compose up -d      # start MySQL, Spring Boot, Nginx and Caddy
docker compose ps
```

The first start takes a minute or two: MySQL creates its database, then Spring Boot starts,
Hibernate creates the tables and `CatalogInitData` fills the catalog. `up -d` waits for that,
because Caddy only starts once the api is healthy. When `docker compose ps` shows all four
containers as `Up`, and `db`, `api` and `frontend` as `(healthy)`, try it on the VM:

```bash
curl -s localhost/api/categories
exit
```

Then open the shop in your browser. On your own machine:

```bash
echo "http://$FQDN"
```

**What runs where:**

| Container | Image | Reachable from |
|-----------|-------|----------------|
| `caddy` | `caddy:2.11-alpine` | The internet, ports 80 and 443 (published by Docker, allowed by the NSG) |
| `frontend` | `<IMAGE_PREFIX>-frontend:<APP_VERSION>` | Only Caddy, through Docker's network |
| `api` | `<IMAGE_PREFIX>-api:<APP_VERSION>` | Only Caddy, through Docker's network |
| `db` | `mysql:8.4` | Only the api, through Docker's network. The data is in the volume `coffeeshop_mysql_data` |

Useful commands on the VM, in `~/coffeeshop`:

```bash
docker compose ps                 # status
docker compose logs -f api        # follow Spring Boot's log (Ctrl+C to stop)
docker compose restart api        # restart one container
docker stats --no-stream          # memory and CPU per container
```

All containers have `restart: unless-stopped`, so they start again by themselves when the VM
reboots.

## 9. HTTPS with a free certificate

Right now the shop uses plain HTTP: names and addresses travel across the internet unencrypted.
Caddy can fix that by itself. When its site address is a domain name instead of `:80`, Caddy
gets a certificate from Let's Encrypt, renews it before it runs out, and redirects `http://` to
`https://`.

Your VM already has a domain name: `$FQDN`. Put it in `.env` on the VM and restart Caddy. On your
machine:

```bash
ssh azureuser@$FQDN "cd coffeeshop && sed -i 's/^SITE_ADDRESS=.*/SITE_ADDRESS=$FQDN/' .env && docker compose up -d"
```

(`$FQDN` is filled in by your own terminal before the command is sent. `sed -i` replaces the
`SITE_ADDRESS` line in the file. `up -d` recreates only the containers whose settings changed:
here only `caddy`.)

Check that Caddy got its certificate:

```bash
ssh azureuser@$FQDN "cd coffeeshop && docker compose logs caddy | grep -i 'certificate obtained'"
```

Then open `https://$FQDN`. The old `http://` address redirects there.

- Let's Encrypt checks that you own the name by connecting to it on **port 80**. Keep the NSG
  rule for port 80, even though the shop now uses 443.
- The certificate is stored in the `caddy_data` volume. Don't delete it with `down -v` again
  and again: Let's Encrypt limits how many certificates you can get for the same name per week.

## 10. Deploy a new version

You changed the code and want it on the server:

1. Commit, push, and tag a new version:

   ```bash
   git tag -a v1.1.0 -m "Product reviews"
   git push origin v1.1.0
   ```

2. Wait for the *Release* workflow. It publishes `:1.1.0`.
3. Switch the VM to the new version:

   ```bash
   ssh azureuser@$FQDN "cd coffeeshop && sed -i 's/^APP_VERSION=.*/APP_VERSION=1.1.0/' .env && docker compose pull && docker compose up -d"
   ```

Compose replaces `api` and `frontend`. MySQL keeps running, and the data stays. Hibernate
(`ddl-auto=update`) adds new tables and columns when the api starts.

**Going back** is the same command with the old version number. Database changes are not undone,
though: a column that 1.1.0 added stays there.

## 11. Stop paying

| Command | What happens | Still costs |
|---------|--------------|-------------|
| `az vm deallocate -g $RG -n $VM` | The VM is stopped and its CPU and memory given back. The disk, IP address and data stay | The disk and the IP: a few dollars a month |
| `az vm start -g $RG -n $VM` | Starts it again. The containers start by themselves, with the same IP and name | |
| `az group delete --name $RG --yes --no-wait` | Deletes **everything** in the resource group: VM, disk, IP, NSG, network, and the data | Nothing |

`az vm stop` alone is not enough: a *stopped* but not *deallocated* VM is still paid for.

Check that nothing is left:

```bash
az group list -o table
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `az vm create`: `RequestDisallowedByAzure` or `RequestDisallowedByPolicy` | The region is not allowed for your subscription. Use one from the list in step 2 |
| `az vm create`: `SkuNotAvailable` | The size is not available in this region. Check another region (step 2), or another size with at least 2 GB, e.g. `Standard_B2s` |
| `az vm create`: `DnsRecordInUse` | Someone already has that `DNS_LABEL` in this region. Choose another one |
| `ssh`: `Connection timed out` | The NSG only allows SSH from `MY_IP`, and your IP has changed (e.g. home vs. school). Update the rule: `az network nsg rule update -g $RG --nsg-name $NSG -n Allow-SSH-from-my-IP --source-address-prefixes $(curl -s https://api.ipify.org)` |
| `ssh`: `Permission denied (publickey)` | SSH doesn't use the key `az vm create` installed. Use it explicitly: `ssh -i ~/.ssh/id_rsa azureuser@$FQDN` |
| `docker: permission denied` on the VM | Log out and in again. If it persists, check that cloud-init has finished: `cloud-init status` and `/var/log/cloud-init-output.log` |
| `docker compose pull`: `denied` or `unauthorized` | The package is still private, or `IMAGE_PREFIX` is wrong. It must be lower case and match the name under *Packages* on GitHub |
| `docker compose pull`: `manifest unknown` | That version was never published. `APP_VERSION` is the tag without `v`, and the Release workflow must have succeeded |
| `exec format error` in the logs | The image was built for ARM (an Apple Silicon Mac) and pushed by hand. Use the images from the Release workflow: they are built for x86-64, like the VM |
| `api` restarts, log says `Access denied for user` | The passwords in `.env` changed after MySQL's first start. MySQL keeps the password it was created with in its volume. Put the old password back, or delete the database with `docker compose down -v` (**all data is lost**) |
| Container killed, `docker compose ps` shows `Exited (137)` | Out of memory. Check `free -h` (is the swap there?) and `docker stats --no-stream` |
| The browser can't connect | Is `caddy` running (`docker compose ps`)? Does the NSG have the 80/443 rule (step 4)? |
| HTTPS: no certificate, Caddy log shows `challenge failed` | Port 80 must be open to the internet, and `SITE_ADDRESS` must be exactly the `$FQDN` (no `http://`, no `/` at the end) |

### Private images

If you'd rather keep the packages private, the VM has to log in to GHCR before `docker compose
pull`:

1. On GitHub: *Settings* → *Developer settings* → *Personal access tokens* → *Tokens (classic)*
   → *Generate new token*, with only the `read:packages` scope.
2. On the VM:

   ```bash
   docker login ghcr.io -u <your-github-user>      # paste the token as the password
   ```

Docker saves the token in `~/.docker/config.json` on the VM, unencrypted. Anyone who can log
in to the VM can read it, so give it no other scopes.

## Security notes

What this setup does well:

- Only ports 80 and 443 are open to everyone. SSH is only open to your IP, and it only accepts
  your key, never a password.
- MySQL has no published port: it can't be reached from outside the VM.
- The passwords are only in `.env`, on your machine and the VM, and never in Git.
- The Spring Boot container runs as an unprivileged user.

What it does **not** do:

- **The shop has no login** (see [Known limitations](../README.md#known-limitations)). Anyone who
  finds the address can see orders and customers by guessing ids. Don't enter real personal data,
  and delete the resource group when you're done.
- There are no backups. Deleting the VM or the volume deletes the orders. One way to make a copy:
  `docker compose exec -T db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" coffeeshop' > backup.sql`
- Updates of Ubuntu and of the `mysql`/`caddy` images are up to you: `sudo apt upgrade` on the
  VM, `docker compose pull && docker compose up -d` for the images.
