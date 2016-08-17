#include "esp_common.h"
#include "driver.h"
#include "i2c.h"

#define LM73_ADDR							0x49
#define LM73_PTR_TEMP					0x00

enum {
	s_lm73_detect,
	s_lm73_read,
	s_lm73_wait,
	s_lm73_error
} lm73_state;

uint8 lm73_tickcnt;
float lm73_temp;
bool lm73_success;
drv_data_t lm73_drv_data;

void i2c_lm73_proc(void) {
	uint16 dat;

	switch (lm73_state) {
		case s_lm73_detect:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(LM73_ADDR));
			if (!i2c_master_checkAck()) {
					lm73_state = s_lm73_error;
			}
			else {
				// set pointer to temperature read
				i2c_master_writeByte(LM73_PTR_TEMP);
				if (!i2c_master_checkAck()) {
						lm73_state = s_lm73_error;
				}
				else {
					lm73_state = s_lm73_read;
				}
			}
			i2c_master_stop();
			break;

		case s_lm73_read:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_READ(LM73_ADDR));
			if (!i2c_master_checkAck()) {
					lm73_state = s_lm73_error;
			}
			else {
				// read temp upper 8-bit
				dat = 0;
				dat = i2c_master_readByte();
				i2c_master_send_ack();
				// 11-bit temperature (2'complement)
				dat = (dat << 3) | (i2c_master_readByte() >> 5);
				i2c_master_send_nack();

				// default lm73 resolution = 0.25
				lm73_temp = (float)dat / 4;
				lm73_success = TRUE;

				lm73_state = s_lm73_wait;
			}
			i2c_master_stop();
			break;

		case s_lm73_wait:
			// 1 second sampling rate
			lm73_tickcnt++;
			if (lm73_tickcnt > I2C_CNT_MS(1000)) {
				lm73_tickcnt = 0;
				lm73_state = s_lm73_detect;
			}
			break;

		case s_lm73_error:
			lm73_temp = 0;
			lm73_success = FALSE;
			lm73_state = s_lm73_detect;
			break;
	}
}

void i2c_lm73_init(void) {
	lm73_drv_data.type = ddt_float;
	lm73_temp = 0;
	lm73_success = FALSE;
	lm73_tickcnt = 0;
	lm73_state = s_lm73_detect;
}

drv_data_t *i2c_lm73_get(void) {
	uint32 temp;

	// update driver data
	lm73_drv_data.value = lm73_temp;
	temp = lm73_temp * DRIVER_DATA_PREC_COEF;
	__snprintf(
		&lm73_drv_data.string[0],
		DRIVER_DATA_STRING_SIZE,
		"%d.%02d",
		temp / DRIVER_DATA_PREC_COEF, temp % DRIVER_DATA_PREC_COEF
	);
	lm73_drv_data.success = lm73_success;

	return &lm73_drv_data;
}
