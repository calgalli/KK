#include "esp_common.h"
#include "lwip/udp.h"
#include "lwip/mem.h"

#define UDP_SERVICE_PORT			8266
#define SECTOR_SIZE					0x1000
#define BOOT_CONFIG_SECTOR	1
#define MAX_ROMS						4

typedef struct {
	uint8 magic;           ///< Our magic, identifies rBoot configuration - should be BOOT_CONFIG_MAGIC
	uint8 version;         ///< Version of configuration structure - should be BOOT_CONFIG_VERSION
	uint8 mode;            ///< Boot loader mode (MODE_STANDARD | MODE_GPIO_ROM)
	uint8 current_rom;     ///< Currently selected ROM (will be used for next standard boot)
	uint8 gpio_rom;        ///< ROM to use for GPIO boot (hardware switch) with mode set to MODE_GPIO_ROM
	uint8 count;           ///< Quantity of ROMs available to boot
	uint8 unused[2];       ///< Padding (not used)
	uint32 roms[MAX_ROMS]; ///< Flash addresses of each ROM
#ifdef BOOT_CONFIG_CHKSUM
	uint8 chksum;          ///< Checksum of this configuration structure (if BOOT_CONFIG_CHKSUM defined)
#endif
} rboot_config;

LOCAL struct udp_pcb *ptel_pcb;
LOCAL os_timer_t restart_timer;

// reboot
void ICACHE_FLASH_ATTR reboot(void) {
	os_timer_disarm(&restart_timer);
	os_timer_setfn(&restart_timer, (os_timer_func_t *)system_restart, NULL);
	os_timer_arm(&restart_timer, 500, 0);
}

// get the rboot config
rboot_config ICACHE_FLASH_ATTR rboot_get_config(void) {
	rboot_config conf;

	spi_flash_read(BOOT_CONFIG_SECTOR * SECTOR_SIZE, (uint32*)&conf, sizeof(rboot_config));
	return conf;
}

// write the rboot config
// preserves the contents of the rest of the sector,
// so the rest of the sector can be used to store user data
// updates checksum automatically (if enabled)
bool ICACHE_FLASH_ATTR rboot_set_config(rboot_config *conf) {
	uint8 *buffer;

	buffer = (uint8*)mem_malloc(SECTOR_SIZE);
	if (buffer == NULL) {
		return NULL;
	}

	if (!buffer) {
		//os_printf("No ram!\r\n");
		return FALSE;
	}

#ifdef BOOT_CONFIG_CHKSUM
	conf->chksum = calc_chksum((uint8*)conf, (uint8*)&conf->chksum);
#endif

	spi_flash_read(BOOT_CONFIG_SECTOR * SECTOR_SIZE, (uint32*)((void*)buffer), SECTOR_SIZE);
	MEMCPY(buffer, conf, sizeof(rboot_config));
	spi_flash_erase_sector(BOOT_CONFIG_SECTOR);
	spi_flash_write(BOOT_CONFIG_SECTOR * SECTOR_SIZE, (uint32*)((void*)buffer), SECTOR_SIZE);

	mem_free(buffer);

	return TRUE;
}

// set current boot rom
bool ICACHE_FLASH_ATTR rboot_set_current_rom(uint8 rom) {
	rboot_config conf;

	conf = rboot_get_config();
	if (rom >= conf.count) {
		return FALSE;
	}
	conf.current_rom = rom;
	return rboot_set_config(&conf);
}

void ICACHE_FLASH_ATTR udp_service_reply(const char *str, uint8 size, struct udp_pcb *pcb, struct ip_addr *addr) {
	struct pbuf *r;

	r = pbuf_alloc(PBUF_TRANSPORT, size, PBUF_RAM);
	MEMCPY(r->payload, str, size);
	r->len = size;
	r->tot_len = size;
	udp_sendto(pcb, r, addr, UDP_SERVICE_PORT);
	pbuf_free(r);
}

void ICACHE_FLASH_ATTR udp_service_recv(void *arg, struct udp_pcb *pcb, struct pbuf *p, struct ip_addr *addr, uint16 port) {
	char *str = (char *)p->payload;
	const char rom_msg[] = "Currently running rom 0\r\n";
	const char switch_msg[] = "rebooting to rom 1...\r\n";

	// test: nc -u 192.168.1.208 8266
  if (p != NULL) {
		if ((p->len == 4) && (str[0] == 'r') && (str[1] == 'o') && (str[2] == 'm')) {
			udp_service_reply(rom_msg, sizeof(rom_msg) - 1, pcb, addr);
		}
		else
		if ((p->len == 7) && (str[0] == 's') && (str[1] == 'w') && (str[2] == 'i') && (str[3] == 't') && (str[4] == 'c') && (str[5] == 'h')) {
			rboot_set_current_rom(1);
			udp_service_reply(switch_msg, sizeof(switch_msg) - 1, pcb, addr);

			// reboot
			reboot();
		}

    pbuf_free(p);
  }
}

void ICACHE_FLASH_ATTR udp_service_init(void) {
	lwip_init();
	ptel_pcb = udp_new();
	udp_bind(ptel_pcb, IP_ADDR_ANY, UDP_SERVICE_PORT);
	udp_recv(ptel_pcb, udp_service_recv, NULL);
	//pbuf_free(ptel_pcb);
}
