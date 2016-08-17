#ifndef __I2C_DS3231_H__
#define __I2C_DS3231_H__

#include "driver.h"

#ifdef __cplusplus
extern "C" {
#endif

drv_data_t *i2c_ds3231_get(void);
drv_data_t *i2c_ds3231_get_date(void);
drv_data_t *i2c_ds3231_get_time(void);

#ifdef __cplusplus
}
#endif

#endif
