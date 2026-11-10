package ek.osnb.demo.coffeeshop.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChatRequest(@NotBlank @Size(max = 2000) String message) {
}
