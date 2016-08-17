#ifndef __I2C_H__
#define __I2C_H__

#ifdef __cplusplus
extern "C" {
#endif

#define I2C_ADDR_WRITE(addr)		(addr << 1)
#define I2C_ADDR_READ(addr)			((addr << 1) | 0x01)
#define I2C_TICK_MS							50
#define I2C_CNT_MS(x)						(x / I2C_TICK_MS)

#ifdef __cplusplus
}
#endif

#endif
