'use strict';

goog.provide('Blockly.KidBright');

goog.require('Blockly.Generator');

Blockly.KidBright = new Blockly.Generator('KidBright');

Blockly.KidBright.taskNumber = 0;

Blockly.KidBright.addReservedWords(
  'Blockly,' + // In case JS is evaled in the current window.
  // https://developer.mozilla.org/en/KidBright/Reference/Reserved_Words
  'break,case,catch,continue,debugger,default,delete,do,else,finally,for,function,if,in,instanceof,new,return,switch,this,throw,try,typeof,var,void,while,with,' +
  'class,enum,export,extends,import,super,implements,interface,let,package,private,protected,public,static,yield,' +
  'const,null,true,false,'
);

/**
 * Order of operation ENUMs.
 * https://developer.mozilla.org/en/KidBright/Reference/Operators/Operator_Precedence
 */
Blockly.KidBright.ORDER_ATOMIC = 0; // 0 "" ...
Blockly.KidBright.ORDER_MEMBER = 1; // . []
Blockly.KidBright.ORDER_FUNCTION_CALL = 2; // ()
Blockly.KidBright.ORDER_INCREMENT = 3; // ++
Blockly.KidBright.ORDER_DECREMENT = 3; // --
Blockly.KidBright.ORDER_LOGICAL_NOT = 4; // !
Blockly.KidBright.ORDER_BITWISE_NOT = 4; // ~
Blockly.KidBright.ORDER_UNARY_PLUS = 4; // +
Blockly.KidBright.ORDER_UNARY_NEGATION = 4; // -
Blockly.KidBright.ORDER_TYPEOF = 4; // typeof
Blockly.KidBright.ORDER_VOID = 4; // void
Blockly.KidBright.ORDER_MULTIPLICATION = 5; // *
Blockly.KidBright.ORDER_DIVISION = 5; // /
Blockly.KidBright.ORDER_MODULUS = 5; // %
Blockly.KidBright.ORDER_ADDITION = 6; // +
Blockly.KidBright.ORDER_SUBTRACTION = 6; // -
Blockly.KidBright.ORDER_BITWISE_SHIFT = 7; // << >> >>>
Blockly.KidBright.ORDER_RELATIONAL = 8; // < <= > >=
Blockly.KidBright.ORDER_EQUALITY = 9; // == != === !==
Blockly.KidBright.ORDER_BITWISE_AND = 10; // &
Blockly.KidBright.ORDER_BITWISE_XOR = 11; // ^
Blockly.KidBright.ORDER_BITWISE_OR = 12; // |
Blockly.KidBright.ORDER_LOGICAL_AND = 13; // &&
Blockly.KidBright.ORDER_LOGICAL_OR = 14; // ||
Blockly.KidBright.ORDER_CONDITIONAL = 15; // ?:
Blockly.KidBright.ORDER_ASSIGNMENT = 16; // = += -= *= /= %= <<= >>= ...
Blockly.KidBright.ORDER_COMMA = 17; // ,
Blockly.KidBright.ORDER_NONE = 99; // (...)

/**
 * Initialise the database of variable names.
 * @param {!Blockly.Workspace} workspace Workspace to generate code from.
 */
Blockly.KidBright.init = function(workspace) {
  // Create a dictionary of definitions to be printed before the code.
  Blockly.KidBright.definitions_ = Object.create(null);
  // Create a dictionary mapping desired function names in definitions_
  // to actual function names (to avoid collisions with user functions).
  Blockly.KidBright.functionNames_ = Object.create(null);

  if (!Blockly.KidBright.variableDB_) {
    Blockly.KidBright.variableDB_ =
      new Blockly.Names(Blockly.KidBright.RESERVED_WORDS_);
  } else {
    Blockly.KidBright.variableDB_.reset();
  }

  var defvars = [];
  var variables = Blockly.Variables.allVariables(workspace);
  if (variables.length) {
    for (var i = 0; i < variables.length; i++) {
      defvars[i] = Blockly.KidBright.variableDB_.getName(variables[i],
        Blockly.Variables.NAME_TYPE);
    }
    Blockly.KidBright.definitions_['variables'] =
      'var ' + defvars.join(', ') + ';';
  }
};

/**
 * Prepend the generated code with the variable definitions.
 * @param {string} code Generated code.
 * @return {string} Completed code.
 */
Blockly.KidBright.finish = function(code) {
  // Convert the definitions dictionary into a list.
  var definitions = [];
  for (var name in Blockly.KidBright.definitions_) {
    definitions.push(Blockly.KidBright.definitions_[name]);
  }
  // Clean up temporary data.
  delete Blockly.KidBright.definitions_;
  delete Blockly.KidBright.functionNames_;
  Blockly.KidBright.variableDB_.reset();
  return definitions.join('\n\n') + '\n\n\n' + code;
};

/**
 * Naked values are top-level blocks with outputs that aren't plugged into
 * anything.  A trailing semicolon is needed to make this legal.
 * @param {string} line Line of generated code.
 * @return {string} Legal line of code.
 */
Blockly.KidBright.scrubNakedValue = function(line) {
  return line + ';\n';
};

/**
 * Encode a string as a properly escaped KidBright string, complete with
 * quotes.
 * @param {string} string Text to encode.
 * @return {string} KidBright string.
 * @private
 */
Blockly.KidBright.quote_ = function(string) {
  // TODO: This is a quick hack.  Replace with goog.string.quote
  string = string.replace(/\\/g, '\\\\')
    .replace(/\n/g, '\\\n')
    .replace(/'/g, '\\\'');
  return '\'' + string + '\'';
};

/**
 * Common tasks for generating KidBright from blocks.
 * Handles comments for the specified block and any connected value blocks.
 * Calls any statements following this block.
 * @param {!Blockly.Block} block The current block.
 * @param {string} code The KidBright code created for this block.
 * @return {string} KidBright code with comments and subsequent blocks added.
 * @private
 */
Blockly.KidBright.scrub_ = function(block, code) {
  var commentCode = '';
  // Only collect comments for blocks that aren't inline.
  if (!block.outputConnection || !block.outputConnection.targetConnection) {
    // Collect comment for this block.
    var comment = block.getCommentText();
    if (comment) {
      commentCode += Blockly.KidBright.prefixLines(comment, '// ') + '\n';
    }
    // Collect comments for all value arguments.
    // Don't collect comments for nested statements.
    for (var x = 0; x < block.inputList.length; x++) {
      if (block.inputList[x].type == Blockly.INPUT_VALUE) {
        var childBlock = block.inputList[x].connection.targetBlock();
        if (childBlock) {
          var comment = Blockly.KidBright.allNestedComments(childBlock);
          if (comment) {
            commentCode += Blockly.KidBright.prefixLines(comment, '// ');
          }
        }
      }
    }
  }
  var nextBlock = block.nextConnection && block.nextConnection.targetBlock();
  var nextCode = Blockly.KidBright.blockToCode(nextBlock);
  return commentCode + code + nextCode;
};

Blockly.KidBright.resetTaskNumber = function() {
	Blockly.KidBright.taskNumber = 0;
}
