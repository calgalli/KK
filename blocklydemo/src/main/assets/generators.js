'use strict';

goog.require('Blockly.KidBright');

// =============================================================================
// basic
// =============================================================================
goog.provide('Blockly.KidBright.basic');

Blockly.KidBright['basic_led16x8'] = function(block) {
	var buf = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00];
	for (var x = 0; x < 16; x++) {
		var byte = 0;
		for (var y = 0; y < 8; y++) {
			var val = block.getFieldValue('POS_X' + x + '_Y' + y);
			if (val == 'TRUE') {
				byte |= (0x01 << y);
			};
		}
		buf[x] = byte;
	}

	// swap buffer for adafruit 8x16
	// 0 = 0, 1 = 8, 2 = 1, 3 = 9, 4 = 2, 5 = 10, 6 = 3, 7 = 11
	// 8 = 4, 9 = 12, 10 = 5, 11 = 13, 12 = 6, 13 = 14, 14 = 7, 15 = 15
	var tmp_buf = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00];
	for (var i = 0; i < 16; i++) {
		if ((i % 2) == 0) {
			tmp_buf[i] = buf[parseInt(i / 2)];
		} else {
			tmp_buf[i] = buf[parseInt(i / 2) + 8];
		}
	}

	var str = '';
	for (var i = 0; i < 16; i++) {
		str += '\\x' + tmp_buf[i].toString(16);;
	}

	return 'i2c_ht16k33_show("' + str + '");\n';
};

Blockly.KidBright['basic_delay'] = function(block) {
	return 'vTaskDelay(' + parseInt(1000 * parseFloat(block.getFieldValue('VALUE'))) + ' / portTICK_RATE_MS);\n';
};

Blockly.KidBright['basic_forever'] = function(block) {
	return 'while(1) {\n' + Blockly.KidBright.statementToCode(block, 'HANDLER') + '}\n';
};

// =============================================================================
// math
// =============================================================================
goog.provide('Blockly.KidBright.math');

Blockly.KidBright['math_number'] = function(block) {
	var num = block.getFieldValue('VALUE');

	//integer`string`driver_data
	return [
		num + '`' + num + '`' + '', Blockly.KidBright.ORDER_ATOMIC
	];

	// Numeric value.
	/*var code = parseFloat(block.getFieldValue('NUM'));
	return [code, Blockly.JavaScript.ORDER_ATOMIC];*/
};

Blockly.KidBright['math_string'] = function(block) {
	var str = block.getFieldValue('VALUE');

	//integer`string`driver_data
	return [
		'' + '`' + str + '`' + '', Blockly.KidBright.ORDER_ATOMIC
	];
};

// =============================================================================
// logic
// =============================================================================
goog.provide('Blockly.KidBright.logic');

Blockly.KidBright['controls_if'] = function(block) {
	// If/elseif/else condition.
	var n = 0;
	var argument = Blockly.KidBright.valueToCode(block, 'IF' + n, Blockly.KidBright.ORDER_NONE) || '0';
	var branch = Blockly.KidBright.statementToCode(block, 'DO' + n);
	var code = 'if (' + argument + ') {\n' + branch + '}';
	
	for (n = 1; n <= block.elseifCount_; n++) {
		argument = Blockly.KidBright.valueToCode(block, 'IF' + n, Blockly.KidBright.ORDER_NONE) || '0';
		branch = Blockly.KidBright.statementToCode(block, 'DO' + n);
		code += ' else if (' + argument + ') {\n' + branch + '}';
	}

	if (block.elseCount_) {
		branch = Blockly.KidBright.statementToCode(block, 'ELSE');
		code += ' else {\n' + branch + '}';
	}

	return code + '\n';
};

Blockly.KidBright['logic_compare'] = function(block) {
	// Comparison operator.
	var OPERATORS = {
		'EQ': '==',
		'NEQ': '!=',
		'LT': '<',
		'LTE': '<=',
		'GT': '>',
		'GTE': '>='
	};
	var operator = OPERATORS[block.getFieldValue('OP')];
	var order = (operator == '==' || operator == '!=') ?
		Blockly.KidBright.ORDER_EQUALITY : Blockly.KidBright.ORDER_RELATIONAL;
	var argument0 = Blockly.KidBright.valueToCode(block, 'A', order) || '0';
	var argument1 = Blockly.KidBright.valueToCode(block, 'B', order) || '0';
	var code = argument0 + ' ' + operator + ' ' + argument1;
	return [code, order];
};

// =============================================================================
// music
// =============================================================================
goog.provide('Blockly.KidBright.music');

Blockly.KidBright['music_note'] = function(block) {
	var ret =
		'sound_note(' + block.getFieldValue('NOTE') + ');\n' +
		'sound_rest(' + block.getFieldValue('DURATION') + ');\n' +
		'sound_off();\n';

	return ret;
};

Blockly.KidBright['music_rest'] = function(block) {
	return 'sound_rest(' + block.getFieldValue('DURATION') + ');\n';
};

Blockly.KidBright['music_set_tempo'] = function(block) {
	return 'sound_set_bpm(' + block.getFieldValue('VALUE') + ');\n';
};

// =============================================================================
// sensor
// =============================================================================
goog.provide('Blockly.KidBright.sensor');

Blockly.KidBright['sensor_lm73'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'i2c_lm73_get()', Blockly.KidBright.ORDER_ATOMIC
	];
};

Blockly.KidBright['sensor_ldr'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'ldr_get()', Blockly.KidBright.ORDER_ATOMIC
	];
};

Blockly.KidBright['sensor_button1'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'button1_get()', Blockly.KidBright.ORDER_ATOMIC
	];
};

Blockly.KidBright['sensor_button2'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'button2_get()', Blockly.KidBright.ORDER_ATOMIC
	];
};

// =============================================================================
// rtc
// =============================================================================
goog.provide('Blockly.KidBright.rtc');

Blockly.KidBright['rtc_get'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'i2c_ds3231_get()', Blockly.KidBright.ORDER_ATOMIC
	];
};
Blockly.KidBright['rtc_get_date'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'i2c_ds3231_get_date()', Blockly.KidBright.ORDER_ATOMIC
	];
};
Blockly.KidBright['rtc_get_time'] = function(block) {
	//integer`string`driver_data
	return [
		'' + '`' + '' + '`' + 'i2c_ds3231_get_time()', Blockly.KidBright.ORDER_ATOMIC
	];
};

// =============================================================================
// comm
// =============================================================================
goog.provide('Blockly.KidBright.comm');

Blockly.KidBright['_os_printf'] = function(arg, newline) {
	var code = '';
	var arglst = arg.split('`');
	if (arglst[0] != '') {
		// integer
		code = 'os_printf("%d' + newline + '", ' + arglst[0] + ');\n';
	} else
	if (arglst[1] != '') {
		// string
		code = 'os_printf("%s' + newline + '", "' + arglst[1] + '");\n';
	} else
	if (arglst[2] != '') {
		// driver data
		code =
			'os_printf("%s' + newline + '", ' + arglst[2] + '->string);\n';
	}

	return code;
}

Blockly.KidBright['comm_uart_write'] = function(block) {
	var argument0 = Blockly.KidBright.valueToCode(block, 'VALUE', Blockly.KidBright.ORDER_ASSIGNMENT) || '0';

	return Blockly.KidBright['_os_printf'](argument0, '');
};

Blockly.KidBright['comm_uart_writeln'] = function(block) {
	var argument0 = Blockly.KidBright.valueToCode(block, 'VALUE', Blockly.KidBright.ORDER_ASSIGNMENT) || '0';

	return Blockly.KidBright['_os_printf'](argument0, '\\n');
};

// =============================================================================
// advance
// =============================================================================
goog.provide('Blockly.KidBright.advance');

Blockly.KidBright['advance_task'] = function(block) {
	// generate unique function name
	Blockly.KidBright.taskNumber++;
	var funcName = 'vTask' + Blockly.KidBright.taskNumber;

	var branch = Blockly.KidBright.statementToCode(block, 'STACK');
	if (Blockly.KidBright.STATEMENT_PREFIX) {
		branch = Blockly.KidBright.prefixLines(
			Blockly.KidBright.STATEMENT_PREFIX.replace(/%1/g,
				'\'' + block.id + '\''), Blockly.KidBright.INDENT) + branch;
	}
	if (Blockly.KidBright.INFINITE_LOOP_TRAP) {
		branch = Blockly.KidBright.INFINITE_LOOP_TRAP.replace(/%1/g,
			'\'' + block.id + '\'') + branch;
	}
	var code = 'LOCAL void ' + funcName + '(void *pvParameters) {\n' +
		branch +
		'  // kill itself\n' +
		'  vTaskDelete(NULL);\n' +
		'}';
	code = Blockly.KidBright.scrub_(block, code);
	// Add % so as not to collide with helper functions in definitions list.
	Blockly.KidBright.definitions_['%' + funcName] = code;
	return null;
};

//-------------------------- Generator function from background_complier.html ----------------------

function generate(blocklyxml) {
      // Parse the XML into a tree.

      var xmlText = blocklyxml;
      var dom;
      console.log(blocklyxml);
      try {
        dom = Blockly.Xml.textToDom(xmlText)
      } catch (e) {
        alert(e);
        return;
      }
      // Create a headless workspace.
      var workspace = new Blockly.Workspace();
      try {
        Blockly.Xml.domToWorkspace(dom, workspace);
        //var code = Blockly.JavaScript.workspaceToCode(workspace);
        var code_str = Blockly.KidBright.workspaceToCode(workspace);


        // extract setup and task statements
        var code_strlst = code_str.split('\n');
        var braces_cnt = 0;
        var task_code = '';
        var task_fn_name = [];
        var setup_code = '// setup\n';
        var in_func_flag = false;
        for (var code_str_index in code_strlst) {
            var line = code_strlst[code_str_index].replace('\n', '');
        if (line.length <= 0) {
            continue;
        }

        if (line.substring(0, 10) == 'LOCAL void') {
            var tmp_line = line.replace('(', ' ');
            var tmp_linelst = tmp_line.split(' ');
            task_fn_name.push(tmp_linelst[2]);
            task_code += '\n';
            in_func_flag = true;
        }

        var open_brace_cnt = line.split('{').length - 1;
        var close_brace_cnt = line.split('}').length - 1;
        braces_cnt = braces_cnt + open_brace_cnt - close_brace_cnt;

        if (in_func_flag) {
            task_code += (line + '\n');
            if (braces_cnt == 0) {
                in_func_flag = false;
            }
        } else {
            setup_code += (line + '\n');
        }
      }

		// add setup code indent
		var setup_code_list = setup_code.split('\n');
		setup_code = '';
		for (var setup_code_index in setup_code_list) {
			setup_code += '  ' + setup_code_list[setup_code_index] + '\n';
		}

    var task_fn_code = '  // create tasks\n';
    for (var task_fn_index in task_fn_name) {
      var task_fn = task_fn_name[task_fn_index];
      task_fn_code += '  xTaskCreate(' + task_fn + ', "' + task_fn + '", USER_STACK_SIZE_MIN, NULL, USER_TASK_PRIORITY, NULL);\n'
    }

    var user_app_code =
      '#include "esp_common.h"\n' +
      '#include "user_config.h"\n' +
      task_code + '\n' +
      'void user_app(void) {\n' +
      setup_code +
      task_fn_code +
      '}';


    Blockly.KidBright.functionNumber = 0;





        BlocklyJavascriptInterface.execute(user_app_code);
      } catch (e) {
        console.log(e.message);
        BlocklyJavascriptInterface.execute("");
      }
    }