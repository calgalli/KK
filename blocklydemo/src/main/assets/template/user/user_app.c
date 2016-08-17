#include "esp_common.h"
#include "user_config.h"
#include "i2c_ds3231.h"
#include "i2c_lm73.h"
#include "ldr.h"
#include "button12.h"

LOCAL void vTask1(void *pvParameters) {
  while(1) {
    i2c_ht16k33_show("\x0\x18\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x18\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
    i2c_ht16k33_show("\x0\x24\x0\x18\x0\x0\x0\x0\x0\x0\x0\x0\x18\x0\x24\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
    i2c_ht16k33_show("\x0\x42\x0\x42\x0\x3c\x0\x0\x0\x0\x3c\x0\x42\x0\x42\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
    i2c_ht16k33_show("\x0\x81\x0\x81\x0\x81\x0\x7e\x7e\x0\x81\x0\x81\x0\x81\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
    i2c_ht16k33_show("\x0\x42\x0\x42\x0\x3c\x0\x0\x0\x0\x3c\x0\x42\x0\x42\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
    i2c_ht16k33_show("\x0\x24\x0\x18\x0\x0\x0\x0\x0\x0\x0\x0\x18\x0\x24\x0");
    vTaskDelay(100 / portTICK_RATE_MS);
  }
  // kill itself
  vTaskDelete(NULL);
}

void user_app(void) {
  // setup
  
  // create tasks
  xTaskCreate(vTask1, "vTask1", USER_STACK_SIZE_MIN, NULL, USER_TASK_PRIORITY, NULL);
}