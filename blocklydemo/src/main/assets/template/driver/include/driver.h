#ifndef __DRIVER_H__
#define __DRIVER_H__

#define DRIVER_DATA_STRING_SIZE		32
#define DRIVER_DATA_PREC_COEF			100	// 100 = 2 decimal digits
#define DRIVER_DATA_U8ARRAY_SIZE	8

#define IS_FLAG_SET(x, b)					(x & b)
#define FLAG_SET(x, b)						(x |= b)
#define FLAG_CLR(x, b)						(x &= (~b))

#define DRIVER_TICK_MS						50
#define DRIVER_CNT_MS(x)					(x / DRIVER_TICK_MS)

#ifdef __cplusplus
extern "C" {
#endif

//typedef void (*t_void_func)(void);

typedef enum drv_data_type {
	ddt_integer, ddt_float, ddt_datetime, ddt_date, ddt_time
} drv_data_type_t;

typedef struct drv_data {
	drv_data_type_t type;
	float value;
	char string[DRIVER_DATA_STRING_SIZE + 1];
	uint8 u8_array[DRIVER_DATA_U8ARRAY_SIZE];
	bool success;
} drv_data_t;

#ifdef __cplusplus
}
#endif

#endif
