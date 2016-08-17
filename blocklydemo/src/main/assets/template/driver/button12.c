#include "esp_common.h"
#include "user_config.h"
#include "gpio.h"
#include "driver.h"

#define	BUTTON2_PIN						2

enum {
	s_button12_get,
	s_button12_check
} button12_state;

uint8 button12_value;
uint8 temp_button12_value;
bool button12_success;
drv_data_t button1_drv_data;
drv_data_t button2_drv_data;

void button12_proc(void) {
	uint8 tmp;

	switch (button12_state) {
		case s_button12_get:
			// check sw1
			temp_button12_value = 0;
			if (!gpio16_input_get()) {
				temp_button12_value |= 0x01;
			}
			// check sw2
			if (!GPIO_INPUT_GET(BUTTON2_PIN)) {
				temp_button12_value |= 0x02;
			}

			button12_state = s_button12_check;
			break;

		case s_button12_check:
			tmp = 0;
			if (!gpio16_input_get()) {
				tmp |= 0x01;
			}
			// check sw2
			if (!GPIO_INPUT_GET(BUTTON2_PIN)) {
				tmp |= 0x02;
			}

			if (tmp == temp_button12_value) {
				// on sw changed
				if (temp_button12_value != button12_value) {
					button12_value = temp_button12_value;
					button12_success = TRUE;
//testbug
//					os_printf("==> button12 = %02X\n",  button12_value);

				}
			}

			button12_state = s_button12_get;
			break;
	}
}

void button12_init(void) {
	// sw1 = gpio16
	gpio16_input_conf();
	// sw2 = gpio2
	GPIO_AS_INPUT(BUTTON2_PIN);

	button1_drv_data.type = ddt_integer;
	button1_drv_data.value = 0;
	button1_drv_data.string[0] = '\x0';
	button1_drv_data.success = FALSE;

	button2_drv_data.type = ddt_integer;
	button2_drv_data.value = 0;
	button2_drv_data.string[0] = '\x0';
	button2_drv_data.success = FALSE;

	button12_value = 0;
	button12_success = FALSE;
	button12_state = s_button12_get;
}

drv_data_t *button_get(drv_data_t *button_drv_data, uint8 bit_no) {
	uint8 tmp;

	// update driver data
	tmp = button12_value;
	button_drv_data->value = (tmp >> bit_no) & 0x01;
	__snprintf(
		&(button_drv_data->string[0]),
		DRIVER_DATA_STRING_SIZE,
		"%d",
		(tmp >> bit_no) & 0x01
	);
	button_drv_data->success = button12_success;

	return button_drv_data;
}

drv_data_t *button1_get(void) {
	return button_get(&button1_drv_data, 0);
}

drv_data_t *button2_get(void) {
	return button_get(&button2_drv_data, 1);
}
