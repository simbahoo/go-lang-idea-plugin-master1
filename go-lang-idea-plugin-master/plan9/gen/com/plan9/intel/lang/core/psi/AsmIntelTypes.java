/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This is a generated file. Not intended for manual editing.
package com.plan9.intel.lang.core.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.plan9.intel.lang.core.lexer.AsmIntelTokenType;
import com.plan9.intel.lang.core.psi.impl.*;

public interface AsmIntelTypes {

  IElementType FRAME_SIZE = new AsmIntelElementType("FRAME_SIZE");
  IElementType FUNCTION = new AsmIntelElementType("FUNCTION");
  IElementType FUNCTION_BODY = new AsmIntelElementType("FUNCTION_BODY");
  IElementType FUNCTION_FLAGS = new AsmIntelElementType("FUNCTION_FLAGS");
  IElementType FUNCTION_HEADER = new AsmIntelElementType("FUNCTION_HEADER");
  IElementType INSTRUCTION_STMT = new AsmIntelElementType("INSTRUCTION_STMT");
  IElementType LITERAL = new AsmIntelElementType("LITERAL");
  IElementType OPERANDS = new AsmIntelElementType("OPERANDS");
  IElementType PREPROCESSOR_DIRECTIVE = new AsmIntelElementType("PREPROCESSOR_DIRECTIVE");
  IElementType STATEMENT = new AsmIntelElementType("STATEMENT");

  IElementType BIT_OR = new AsmIntelTokenType("|");
  IElementType COLON = new AsmIntelTokenType(":");
  IElementType COMMA = new AsmIntelTokenType(",");
  IElementType FLAG = new AsmIntelTokenType("FLAG");
  IElementType HEX = new AsmIntelTokenType("hex");
  IElementType IDENTIFIER = new AsmIntelTokenType("identifier");
  IElementType IMPORT = new AsmIntelTokenType("import");
  IElementType INSTRUCTION = new AsmIntelTokenType("INSTRUCTION");
  IElementType INT = new AsmIntelTokenType("int");
  IElementType LABEL = new AsmIntelTokenType("LABEL");
  IElementType LPAREN = new AsmIntelTokenType("(");
  IElementType PSEUDO_REG = new AsmIntelTokenType("PSEUDO_REG");
  IElementType RPAREN = new AsmIntelTokenType(")");
  IElementType STRING = new AsmIntelTokenType("STRING");
  IElementType TEXT = new AsmIntelTokenType("TEXT");

  class Factory {
    private Factory() {}

    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == FRAME_SIZE) {
        return new AsmIntelFrameSizeImpl(node);
      }
      if (type == FUNCTION) {
        return new AsmIntelFunctionImpl(node);
      }
      if (type == FUNCTION_BODY) {
        return new AsmIntelFunctionBodyImpl(node);
      }
      if (type == FUNCTION_FLAGS) {
        return new AsmIntelFunctionFlagsImpl(node);
      }
      if (type == FUNCTION_HEADER) {
        return new AsmIntelFunctionHeaderImpl(node);
      }
      if (type == INSTRUCTION_STMT) {
        return new AsmIntelInstructionStmtImpl(node);
      }
      if (type == LITERAL) {
        return new AsmIntelLiteralImpl(node);
      }
      if (type == OPERANDS) {
        return new AsmIntelOperandsImpl(node);
      }
      if (type == PREPROCESSOR_DIRECTIVE) {
        return new AsmIntelPreprocessorDirectiveImpl(node);
      }
      if (type == STATEMENT) {
        return new AsmIntelStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
