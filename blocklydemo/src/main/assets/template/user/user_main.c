#include "esp_common.h"
#include "user_config.h"
#include "gpio.h"
#include "uart.h"
#include "wiring.h"

/*
VERBOSE=1 make clean;VERBOSE=1 make BOOT=new APP=1 SPI_SPEED=40 SPI_MODE=QIO SPI_SIZE_MAP=7
*/

LOCAL void vUdpServiceTask(void *pvParameters) {
	struct ip_info ip_config;

	// wait assigned ip address
	wifi_get_ip_info(STATION_IF, &ip_config);
	while(ip_config.ip.addr == 0){
		vTaskDelay(1000 / portTICK_RATE_MS);
		wifi_get_ip_info(STATION_IF, &ip_config);
	}

	// init udp service
	udp_service_init();

	while (1) {
		vTaskDelay(1000 / portTICK_RATE_MS);
	}
}

LOCAL void vUserStartupTask(void *pvParameters) {
	// wait i2c driver ready
	while (!i2c_state_ready()) {
		vTaskDelay(100 / portTICK_RATE_MS);
	}

	// real user app
	user_app();

	// kill itself
	vTaskDelete(NULL);
}

void user_init(void) {
	// uart init
	uart_init_new();
	UART_SetBaudrate(UART0, BIT_RATE_115200);

	// http://patorjk.com/software/taag/#p=display&f=Graceful&t=Kid-Bright
	os_printf("\n\n");
	os_printf(" __ _  __  ____      ____  ____  __  ___  _  _  ____ \n");
	os_printf("(  / )(  )(    \\ ___(  _ \\(  _ \\(  )/ __)/ )( \\(_  _)\n");
	os_printf(" )  (  )(  ) D ((___)) _ ( )   / )(( (_ \\) __ (  )(  \n");
	os_printf("(__\\_)(__)(____/    (____/(__\\_)(__)\\___/\\_)(_/ (__) \n");
	os_printf("SDK version:%s\n", system_get_sdk_version());

	// setup wifi
	wifi_set_opmode(STATION_MODE);

	// create tasks
	xTaskCreate(vUdpServiceTask, "UdpService Task", USER_STACK_SIZE_MIN, NULL, USER_TASK_PRIORITY, NULL);
	device_create_task();
	xTaskCreate(vUserStartupTask, "User Startup Task", USER_STACK_SIZE_MIN, NULL, USER_TASK_PRIORITY, NULL);
}
