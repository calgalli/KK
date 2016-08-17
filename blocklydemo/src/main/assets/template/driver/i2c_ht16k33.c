#include "esp_common.h"
#include "driver.h"
#include "i2c.h"

#define HT16K33_ADDR					0x70
#define HT16K33_OSC_OFF				0x20
#define HT16K33_OSC_ON				0x21
#define HT16K33_DISP_OFF			0x80
#define HT16K33_DISP_ON				0x81
#define HT16K33_DIM_SET_8_16	0xe7
#define HT16K33_DIM_SET_16_16	0xef
#define HT16K33_DISP_ADDR_PTR	0x00

#define HT16K33_SHOW_FLAG			0x01
#define HT16K33_SHIFT_FLAG		0x02

enum {
	s_ht16k33_detect,
	s_ht16k33_clrscr,
	s_ht16k33_cmd_init,
	s_ht16k33_show,
	s_ht16k33_idle,
	s_ht16k33_error
} i2c_ht16k33_state;

uint8 ht16k33_cmd_init[] = {
	HT16K33_OSC_ON,
	HT16K33_DIM_SET_8_16,
	HT16K33_DISP_ON
};

uint8 ht16k33_buffer[16];
uint8 cmd_index, ht16k33_flag;

void i2c_ht16k33_proc(void) {
	uint8 i;
	uint8 tmp_buf[16];

	switch (i2c_ht16k33_state) {
		case s_ht16k33_detect:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(HT16K33_ADDR));
			if (!i2c_master_checkAck()) {
				i2c_ht16k33_state = s_ht16k33_error;
			}
			else {
				i2c_ht16k33_state = s_ht16k33_clrscr;
			}
			i2c_master_stop();
			break;

		case s_ht16k33_clrscr:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(HT16K33_ADDR));
			if (!i2c_master_checkAck()) {
				i2c_ht16k33_state = s_ht16k33_error;
			}
			else {
				i2c_master_writeByte(HT16K33_DISP_ADDR_PTR);
				if (!i2c_master_checkAck()) {
					i2c_ht16k33_state = s_ht16k33_error;
				}
				else {
					for (i=0; i<16; i++) {
						i2c_master_writeByte(0x00);
						if (!i2c_master_checkAck()) {
							//s_ht16k33_error
						}
					}
					cmd_index = 0;
					i2c_ht16k33_state = s_ht16k33_cmd_init;
				}
			}
			i2c_master_stop();
			break;

		case s_ht16k33_cmd_init:
			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(HT16K33_ADDR));
			if (!i2c_master_checkAck()) {
				i2c_ht16k33_state = s_ht16k33_error;
			}
			else {
				i2c_master_writeByte(ht16k33_cmd_init[cmd_index]);
				if (!i2c_master_checkAck()) {
					i2c_ht16k33_state = s_ht16k33_error;
				}
				else {
					cmd_index++;
					if (cmd_index >= 3) {
						i2c_ht16k33_state = s_ht16k33_idle;
					}
				}
			}
			i2c_master_stop();
			break;

		case s_ht16k33_show:
			// prepare temp buf
			MEMCPY(tmp_buf, ht16k33_buffer, sizeof(tmp_buf));

			i2c_master_start();
			i2c_master_writeByte(I2C_ADDR_WRITE(HT16K33_ADDR));
			if (!i2c_master_checkAck()) {
				i2c_ht16k33_state = s_ht16k33_error;
			}
			else {
				i2c_master_writeByte(HT16K33_DISP_ADDR_PTR);
				if (!i2c_master_checkAck()) {
					i2c_ht16k33_state = s_ht16k33_error;
				}
				else {
					for (i=0; i<16; i++) {
						i2c_master_writeByte(tmp_buf[i]);
						if (!i2c_master_checkAck()) {
							//s_ht16k33_error
							i2c_ht16k33_state = s_ht16k33_error;
						}
					}
					// if not error, goto idle state
					if (i2c_ht16k33_state != s_ht16k33_error) {
						i2c_ht16k33_state = s_ht16k33_idle;
					}
				}
			}
			i2c_master_stop();
			break;

		case s_ht16k33_idle:
			// check pending flag
			if (IS_FLAG_SET(ht16k33_flag, HT16K33_SHOW_FLAG)) {
				FLAG_CLR(ht16k33_flag, HT16K33_SHOW_FLAG);
				i2c_ht16k33_state = s_ht16k33_show;
			}
			break;

		case s_ht16k33_error:
			break;
	}
}

void i2c_ht16k33_init(void) {
	ht16k33_flag = 0;
	i2c_ht16k33_state = s_ht16k33_detect;
}

void i2c_ht16k33_show(const uint8 *buf) {
	// copy into ht16k33 buffer
	MEMCPY(ht16k33_buffer, buf, sizeof(ht16k33_buffer));
	FLAG_SET(ht16k33_flag, HT16K33_SHOW_FLAG);
}
