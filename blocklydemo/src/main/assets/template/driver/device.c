#include "esp_common.h"
#include "i2c_master.h"
#include "i2c.h"
#include "user_config.h"
#include "ldr.h"
#include "button12.h"

enum {
	s_driver_init,
	s_driver_proc
} driver_state;

enum {
	s_i2c_init,
	s_i2c_proc
} i2c_state;

uint8 i2c_state_ready(void) {
	if (i2c_state == s_i2c_proc) {
		return 1;
	} else {
		return 0;
	}
}

LOCAL void vI2CManTask(void *pvParameters) {
	while (1) {
		switch (i2c_state) {
			case s_i2c_init:
				// i2c init
				i2c_master_gpio_init();

				// driver init
				i2c_ht16k33_init();
				i2c_lm73_init();
				i2c_ds3231_init();

				// i2c ready
				i2c_state = s_i2c_proc;
			break;

			case s_i2c_proc:
				i2c_ht16k33_proc();
				i2c_lm73_proc();
				i2c_ds3231_proc();
			break;
		}

		// i2c task state machine delay
		vTaskDelay(I2C_TICK_MS / portTICK_RATE_MS);
	}
}

LOCAL void vDeviceDriverTask(void *pvParameters) {
	while (1) {
		switch (driver_state) {
			case s_driver_init:
				// init devices
				ldr_init();
				button12_init();

				// i2c ready
				driver_state = s_driver_proc;
			break;

			case s_driver_proc:
				ldr_proc();
				button12_proc();
			break;
		}

		// device driver task state machine delay
		vTaskDelay(DRIVER_TICK_MS / portTICK_RATE_MS);
	}
}

void device_create_task(void) {
	driver_state = s_driver_init;
	xTaskCreate(vDeviceDriverTask, "Device Driver Task", USER_STACK_SIZE_MIN, NULL, USER_TASK_PRIORITY, NULL);

	i2c_state = s_i2c_init;
	xTaskCreate(vI2CManTask, "I2C Manager Task", DRV_STACK_SIZE_MIN, NULL, DRV_TASK_PRIORITY, NULL);
}
