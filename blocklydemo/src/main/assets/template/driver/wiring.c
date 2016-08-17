#include "esp_common.h"

#define PIN_PULLDWN_DIS(PIN_NAME) CLEAR_PERI_REG_MASK(PIN_NAME, PERIPHS_IO_MUX_PULLDWN)

const int gpio_pin_register[16] = {
  PERIPHS_IO_MUX_GPIO0_U,
  PERIPHS_IO_MUX_U0TXD_U,
  PERIPHS_IO_MUX_GPIO2_U,
  PERIPHS_IO_MUX_U0RXD_U,
  PERIPHS_IO_MUX_GPIO4_U,
  PERIPHS_IO_MUX_GPIO5_U,
  PERIPHS_IO_MUX_SD_CLK_U,
  PERIPHS_IO_MUX_SD_DATA0_U,
  PERIPHS_IO_MUX_SD_DATA1_U,
  PERIPHS_IO_MUX_SD_DATA2_U,
  PERIPHS_IO_MUX_SD_DATA3_U,
  PERIPHS_IO_MUX_SD_CMD_U,
  PERIPHS_IO_MUX_MTDI_U,
  PERIPHS_IO_MUX_MTCK_U,
  PERIPHS_IO_MUX_MTMS_U,
  PERIPHS_IO_MUX_MTDO_U
};

void ICACHE_FLASH_ATTR pinMode(uint8_t pin, uint8_t mode) {
  if ((1 << pin) & 0b110101) {
    PIN_FUNC_SELECT(gpio_pin_register[pin], 0);
  } else {
    PIN_FUNC_SELECT(gpio_pin_register[pin], 3);
  }

  PIN_PULLDWN_DIS(gpio_pin_register[pin]);
  PIN_PULLUP_EN(gpio_pin_register[pin]);

  if (mode) {
    GPIO_REG_WRITE(GPIO_ENABLE_W1TC_ADDRESS, 1 << pin); // GPIO input
  } else {
    GPIO_REG_WRITE(GPIO_ENABLE_W1TS_ADDRESS, 1 << pin); // GPIO output
  }
}

void ICACHE_FLASH_ATTR digitalWrite(uint8_t pin, uint8_t state) {
  if (state) {
    GPIO_REG_WRITE(GPIO_OUT_W1TS_ADDRESS, 1 << pin); // GPIO high
  } else {
    GPIO_REG_WRITE(GPIO_OUT_W1TC_ADDRESS, 1 << pin); // GPIO low
  }
}

int ICACHE_FLASH_ATTR digitalRead(uint8_t pin) {
  return (GPIO_REG_READ(GPIO_OUT_ADDRESS) >> pin) & 0x01;
}
