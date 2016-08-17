#include "esp_common.h"
#include "driver.h"
#include "i2c.h"

#define DS3231_ADDR							0x68
#define DS3231_PTR_TEMP					0x00

#define DS3231_WRITE_FLAG				0x01

enum {
	s_ds3231_detect,
	s_ds3231_read,
	s_ds3231_wait,
	s_ds3231_write,
	s_ds3231_error
} ds3231_state;

uint8 ds3231_tickcnt;
uint8 ds3231_u8_array[DRIVER_DATA_U8ARRAY_SIZE];
bool ds3231_success;
drv_data_t ds3231_drv_data, ds3231_drv_data_date, ds3231_drv_data_time;
uint8 ds3231_buffer[8], ds3231_flag;

void i2c_ds3231_proc(void) {
	uint8 i, dat;

	switch (ds3231_state) {
		case s_ds3231_detect:
			if (IS_FLAG_SET(ds3231_flag, DS3231_WRITE_FLAG)) {
				FLAG_CLR(ds3231_flag, DS3231_WRITE_FLAG);
				ds3231_state = s_ds3231_write;
			}
			else {
				i2c_master_start();
				i2c_master_writeByte(I2C_ADDR_WRITE(DS3231_ADDR));
				if (!i2c_master_checkAck()) {
						ds3231_state = s_ds3231_error;
				}
				else {
					// set address pointer to 0
					i2c_master_writeByte(0);
					if (!i2c_master_checkAck()) {
							ds3231_state = s_ds3231_error;
					}
					else {
						ds3231_state = s_ds3231_read;
					}
				}
				i2c_master_stop();
			}
			break;

		case s_ds3231_read:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_READ(DS3231_ADDR));
			if (!i2c_master_checkAck()) {
					ds3231_state = s_ds3231_error;
			}
			else {
				// read temp upper 8-bit
				for (i=0; i<6; i++) {
					ds3231_u8_array[i] = i2c_master_readByte();
					i2c_master_send_ack();
				}
				ds3231_u8_array[6] = i2c_master_readByte();
				i2c_master_send_nack();

				ds3231_u8_array[2] &= 0x3f;	// clear hour am/pm bit
				ds3231_u8_array[5] &= 0x7f;	// clear month century bit

				ds3231_state = s_ds3231_wait;
			}
			i2c_master_stop();
			break;

		case s_ds3231_wait:
			// 1 second sampling rate
			ds3231_tickcnt++;
			if (ds3231_tickcnt > I2C_CNT_MS(500)) {
				ds3231_tickcnt = 0;
				ds3231_state = s_ds3231_detect;
//os_printf("==> 20%02x-%02x-%02x %02x:%02x:%02x\n", ds3231_u8_array[6], ds3231_u8_array[5], ds3231_u8_array[4], ds3231_u8_array[2], ds3231_u8_array[1], ds3231_u8_array[0]);
			}
			break;

		case s_ds3231_write:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(DS3231_ADDR));
			if (!i2c_master_checkAck()) {
					ds3231_state = s_ds3231_error;
			}
			else {
				// set address pointer to 0
				i2c_master_writeByte(0);
				if (!i2c_master_checkAck()) {
						ds3231_state = s_ds3231_error;
				}
				else {
					// write rtc
					for (i=0; i<7; i++) {
						i2c_master_writeByte(ds3231_buffer[i]);
						if (!i2c_master_checkAck()) {
								ds3231_state = s_ds3231_error;
						}
					}

					if (ds3231_state != s_ds3231_error) {
						ds3231_state = s_ds3231_detect;
					}
				}
			}
			i2c_master_stop();
			break;

		case s_ds3231_error:
			ds3231_success = FALSE;
			ds3231_state = s_ds3231_detect;
			break;
	}
}

void i2c_ds3231_init(void) {
	uint8 i;

	ds3231_drv_data.type = ddt_datetime;
	ds3231_drv_data_date.type = ddt_date;
	ds3231_drv_data_time.type = ddt_time;
	for (i=0; i<DRIVER_DATA_U8ARRAY_SIZE; i++) {
		ds3231_u8_array[DRIVER_DATA_U8ARRAY_SIZE] = 0;
	}

	ds3231_flag = 0;
	ds3231_success = FALSE;
	ds3231_tickcnt = 0;
	ds3231_state = s_ds3231_detect;

	// get rtc once
	i = 0;
	while ((ds3231_state != s_ds3231_wait) && (i < 8)) {
		i2c_ds3231_proc();
		i++;
	}

/*
//testbug
	ds3231_state = s_ds3231_detect;
	ds3231_buffer[0] = 0;
	ds3231_buffer[1] = 0x34;
	ds3231_buffer[2] = 0x20;
	ds3231_buffer[3] = 0x03;
	ds3231_buffer[4] = 0x05;
	ds3231_buffer[5] = 0x07;
	ds3231_buffer[6] = 0x16;
	FLAG_SET(ds3231_flag, DS3231_WRITE_FLAG);*/
}

drv_data_t *i2c_ds3231_get(void) {
	uint8 i, tmp[DRIVER_DATA_U8ARRAY_SIZE];

	// update driver data
	MEMCPY(tmp, ds3231_u8_array, DRIVER_DATA_U8ARRAY_SIZE);

	__snprintf(
		&ds3231_drv_data.string[0],
		DRIVER_DATA_STRING_SIZE,
		"20%02x-%02x-%02x %02x:%02x:%02x",
		tmp[6], tmp[5], tmp[4], tmp[2], tmp[1], tmp[0]
	);

	for (i=0; i<7; i++) {
		ds3231_drv_data.u8_array[i] = ((tmp[i] & 0xf0) >> 4) * 10 + (tmp[i] & 0x0f);
	}

	ds3231_drv_data.success = ds3231_success;

	return &ds3231_drv_data;
}

drv_data_t *i2c_ds3231_get_date(void) {
	uint8 i, tmp[DRIVER_DATA_U8ARRAY_SIZE];

	// update driver data
	MEMCPY(tmp, ds3231_u8_array, DRIVER_DATA_U8ARRAY_SIZE);

	__snprintf(
		&ds3231_drv_data_date.string[0],
		DRIVER_DATA_STRING_SIZE,
		"20%02x-%02x-%02x",
		tmp[6], tmp[5], tmp[4]
	);

	for (i=0; i<7; i++) {
		ds3231_drv_data_date.u8_array[i] = ((tmp[i] & 0xf0) >> 4) * 10 + (tmp[i] & 0x0f);
	}

	ds3231_drv_data_date.success = ds3231_success;

	return &ds3231_drv_data_date;
}

drv_data_t *i2c_ds3231_get_time(void) {
	uint8 i, tmp[DRIVER_DATA_U8ARRAY_SIZE];

	// update driver data
	MEMCPY(tmp, ds3231_u8_array, DRIVER_DATA_U8ARRAY_SIZE);

	__snprintf(
		&ds3231_drv_data_time.string[0],
		DRIVER_DATA_STRING_SIZE,
		"%02x:%02x:%02x",
		tmp[2], tmp[1], tmp[0]
	);

	for (i=0; i<7; i++) {
		ds3231_drv_data_time.u8_array[i] = ((tmp[i] & 0xf0) >> 4) * 10 + (tmp[i] & 0x0f);
	}

	ds3231_drv_data_time.success = ds3231_success;

	return &ds3231_drv_data_time;
}
