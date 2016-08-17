APP_DATA_PATH=/data/data/com.google.blockly.demo/files
BIN_PATH=$(APP_DATA_PATH)/xtensa-lx106-elf_arm/bin
SDK_PATH=$(APP_DATA_PATH)/ESP8266_RTOS_SDK
ESPTOOL2=$(APP_DATA_PATH)/xtensa-lx106-elf_arm/bin/esptool2

SHELL:=/system/bin/sh

AR = $(BIN_PATH)/xtensa-lx106-elf-ar
CC = $(BIN_PATH)/xtensa-lx106-elf-gcc
NM = $(BIN_PATH)/xtensa-lx106-elf-nm
CPP = $(BIN_PATH)/xtensa-lx106-elf-g++
OBJCOPY = $(BIN_PATH)/xtensa-lx106-elf-objcopy
OBJDUMP = $(BIN_PATH)/xtensa-lx106-elf-objdump

EXEC_FILE=./out/eagle.app.v6.out

BOOT?=new
APP?=1
SPI_SPEED?=40
SPI_MODE?=QIO
SPI_SIZE_MAP?=7



ifeq ($(BOOT), new)
    boot = new
else
    ifeq ($(BOOT), old)
        boot = old
    else
        boot = none
    endif
endif

ifeq ($(APP), 1)
    app = 1
else
    ifeq ($(APP), 2)
        app = 2
    else
        app = 0
    endif
endif

ifeq ($(SPI_SPEED), 26.7)
    freqdiv = 1
else
    ifeq ($(SPI_SPEED), 20)
        freqdiv = 2
    else
        ifeq ($(SPI_SPEED), 80)
            freqdiv = 15
        else
            freqdiv = 0
        endif
    endif
endif


ifeq ($(SPI_MODE), QOUT)
    mode = 1
else
    ifeq ($(SPI_MODE), DIO)
        mode = 2
    else
        ifeq ($(SPI_MODE), DOUT)
            mode = 3
        else
            mode = 0
        endif
    endif
endif

addr = 0x01000

ifeq ($(SPI_SIZE_MAP), 1)
  size_map = 1
  flash = 256
else
  ifeq ($(SPI_SIZE_MAP), 2)
    size_map = 2
    flash = 1024
    ifeq ($(app), 2)
      addr = 0x81000
    endif
  else
    ifeq ($(SPI_SIZE_MAP), 3)
      size_map = 3
      flash = 2048
      ifeq ($(app), 2)
        addr = 0x81000
      endif
    else
      ifeq ($(SPI_SIZE_MAP), 4)
        size_map = 4
        flash = 4096
        ifeq ($(app), 2)
          addr = 0x81000
        endif
      else
        ifeq ($(SPI_SIZE_MAP), 5)
          size_map = 5
          flash = 2048
          ifeq ($(app), 2)
            addr = 0x101000
          endif
        else
          ifeq ($(SPI_SIZE_MAP), 6)
            size_map = 6
            flash = 4096
            ifeq ($(app), 2)
              addr = 0x101000
            endif
          else

            ifeq ($(SPI_SIZE_MAP), 7)
              size_map = 7
              flash = 1024
              ifeq ($(app), 2)
                addr = 0x101000
              endif
            else

              size_map = 0
              flash = 512
              ifeq ($(app), 2)
                addr = 0x41000
              endif

            endif


          endif
        endif
      endif
    endif
  endif
endif

LDDIR = $(SDK_PATH)/ld

LD_FILE = $(LDDIR)/eagle.app.v6.ld

ifneq ($(boot), none)
ifneq ($(app),0)
    ifeq ($(size_map), 7)
      LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).1024.app$(app).rboot.ld
    else

      ifeq ($(size_map), 6)
        LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).2048.ld
      else
        ifeq ($(size_map), 5)
          LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).2048.ld
        else
          ifeq ($(size_map), 4)
            LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).1024.app$(app).ld
          else
            ifeq ($(size_map), 3)
              LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).1024.app$(app).ld
            else
              ifeq ($(size_map), 2)
                LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).1024.app$(app).ld
              else
                ifeq ($(size_map), 0)
                  LD_FILE = $(LDDIR)/eagle.app.v6.$(boot).512.app$(app).ld
                endif
              endif
  	      endif
  	    endif
  	  endif

    endif
	endif
	BIN_NAME = user$(app).$(flash).$(boot).$(size_map)
endif
else
    app = 0
endif

INCLUDES := $(INCLUDES) -I $(SDK_PATH)/include -I $(SDK_PATH)/extra_include
INCLUDES += -I $(SDK_PATH)/include/espressif
INCLUDES += -I $(SDK_PATH)/include/lwip
INCLUDES += -I $(SDK_PATH)/include/lwip/ipv4
INCLUDES += -I $(SDK_PATH)/include/lwip/ipv6
INCLUDES += -I $(SDK_PATH)/include/nopoll
INCLUDES += -I $(SDK_PATH)/include/spiffs
INCLUDES += -I $(SDK_PATH)/include/ssl
INCLUDES += -I $(SDK_PATH)/include/json
INCLUDES += -I ./driver/include
INCLUDES += -I ./include




CCFLAGS += 			\
	-g			\
	-Wpointer-arith		\
	-Wundef			\
	-Werror			\
	-Wl,-EL			\
	-fno-inline-functions	\
	-nostdlib       \
	-mlongcalls	\
	-mtext-section-literals \
	-ffunction-sections \
	-fdata-sections
#	-Wall



LINKFLAGS_eagle.app.v6 = \
	-L$(SDK_PATH)/lib        \
	-Wl,--gc-sections   \
	-nostdlib	\
    -T$(LD_FILE)   \
	-Wl,--no-check-sections	\
    -u call_user_start	\
	-Wl,-static						\
	-Wl,--start-group					\
	-lcirom \
	-lcrypto	\
	-lespconn	\
	-lespnow	\
	-lfreertos	\
	-lgcc					\
	-lhal					\
	-ljson	\
	-llwip	\
	-lmain	\
	-lmesh	\
	-lmirom	\
	-lnet80211	\
	-lnopoll	\
	-lphy	\
	-lpp	\
	-lpwm	\
	-lsmartconfig	\
	-lspiffs	\
	-lssl	\
	-lwpa	\
	-lwps		\
	$(DEP_LIBS_eagle.app.v6)					\
	-Wl,--end-group


CFLAGS = $(CCFLAGS) $(DEFINES) $(EXTRA_CCFLAGS) $(INCLUDES)

# Make does not offer a recursive wildcard function, so here's one:
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))
# How to recursively find all files that match a pattern
SRCS := $(call rwildcard,./,*.c)

OBJS:=$(SRCS:%.c=%.o)

objects = $(patsubst %.c,obj/%.o,$(notdir $(SRCS)))

all:$(EXEC_FILE)
	@echo ============================**
	@echo $(OBJS)
	@echo ****************************
	@echo $(OBJS)
	#$(CC) $(LINKFLAGS_eagle.app.v6) $(objects) -o ./out/eagle.app.v6.out
	#$(ESPTOOL2) -quiet -bin -boot2  ./out/eagle.app.v6.out ./out/rom0.bin .text .data .rodata

$(EXEC_FILE): $(OBJS)
	$(CC) $(LINKFLAGS_eagle.app.v6) $(OBJS) -o $(EXEC_FILE)
	$(ESPTOOL2) -quiet -bin -boot2  $(EXEC_FILE) ./out/rom0.bin .text .data .rodata

%.o:%.c
	@echo $(objects)
	@echo ****************************
	@echo $(OBJS)
	#$(CC) -c $(CFLAGS) $< -o $(patsubst %.o,obj/%.o , $(notdir $@))
	#(CC) -c $(CFLAGS) $< -o $(patsubst %.o,obj/%.o , $(notdir $@))
	$(CC) -c $(CFLAGS) $^ -o $@

	#$(CC) -c $(CFLAGS) $< -o $(objects) , $(notdir $@)) $^ -o $@
clean:
	rm ./out/*
