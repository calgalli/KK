#include "esp_common.h"
#include "user_config.h"

uint32 bpm = 120;

const uint32 music_notes_freq[] = {
	261, // C4
	277, // C#4
	293, // D4
	311, // Eb4
	329, // E4
	349, // F4
	369, // F#4
	391, // G4
	415, // G#4
	440, // A4
	466, // Bb4
	493, // B4
	523 // C5
};

void sound_on(uint32 freq, uint8 duty_in_percent) {
	uint32 pinInfoList[3] = {PERIPHS_IO_MUX_MTCK_U, FUNC_GPIO13, PWM_PIN};
	uint32 duty = 512; // default duty cycle = 50%

	if (duty_in_percent <= 100) {
		duty = (duty_in_percent * 1024) / 100;
	}

	pwm_init(1e6/freq, &duty, PWM_CHANNEL, &pinInfoList);
	pwm_start();
	pinMode(PWM_PIN, 0); // output
}

void sound_off(void) {
	pinMode(PWM_PIN, 1); // input
}

void sound_note(uint8 note) {
	if (note < sizeof(music_notes_freq)) {
		sound_on(music_notes_freq[note], 50);
	}
}

void sound_rest(uint8 duration) {
	uint32 quarter_delay;
	uint32 delay = 0;
	/*
	[120 bpm]
	whole = 2000 ms
	haft = 1000 ms
	quarter delay = 60*1000/120 = 500 ms
	eighth = 250 ms
	sixteenth = 125
	*/
	quarter_delay = 60*1000/bpm;
	switch (duration) {
		case 0:
			delay = 4 * quarter_delay;
			break;
		case 1:
			delay = 2 * quarter_delay;
			break;
		case 2:
			delay = quarter_delay;
			break;
		case 3:
			delay = quarter_delay / 2;
			break;
		case 4:
			delay = quarter_delay / 4;
			break;
	}

	if (delay > 0) {
		vTaskDelay(delay / portTICK_RATE_MS);
	}
}

uint32 sound_get_bpm(void) {
	return bpm;
}

void sound_set_bpm(uint32 val) {
	bpm = val;
}
