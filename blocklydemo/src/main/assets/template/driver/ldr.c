#include "esp_common.h"
#include "user_config.h"
#include "driver.h"

#define MAX_LDR_VALUE			326

enum {
	s_ldr_read,
	s_ldr_wait
} ldr_state;

uint8 ldr_tickcnt;
uint16 ldr_value;
bool ldr_success;
drv_data_t ldr_drv_data;

void ldr_proc(void) {
	uint16 dat;

	switch (ldr_state) {
		case s_ldr_read:
		 	dat = system_adc_read();
			if (dat > MAX_LDR_VALUE) {
				dat = MAX_LDR_VALUE;
			}
			ldr_value = ((MAX_LDR_VALUE - dat)*100)/MAX_LDR_VALUE;

//testbug
//os_printf("====> %d\n", ldr_value);

			ldr_success = TRUE;
			ldr_state = s_ldr_wait;
			break;

		case s_ldr_wait:
			// 1 second sampling rate
			ldr_tickcnt++;
			if (ldr_tickcnt > DRIVER_CNT_MS(1000)) {
				ldr_tickcnt = 0;
				ldr_state = s_ldr_read;
			}
			break;
	}
}

void ldr_init(void) {
	ldr_drv_data.type = ddt_integer;
	ldr_value = 0;
	ldr_success = FALSE;
	ldr_tickcnt = 0;
	ldr_state = s_ldr_read;
}

drv_data_t *ldr_get(void) {
	uint16 temp;

	// update driver data
	temp = ldr_value;
	ldr_drv_data.value = temp;
	__snprintf(
		&ldr_drv_data.string[0],
		DRIVER_DATA_STRING_SIZE,
		"%d",
		temp
	);
	ldr_drv_data.success = ldr_success;

	return &ldr_drv_data;
}
