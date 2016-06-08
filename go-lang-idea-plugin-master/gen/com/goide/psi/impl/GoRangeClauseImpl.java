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
package com.goide.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.goide.psi.GoPsiTreeUtil;
import static com.goide.GoTypes.*;
import com.goide.psi.*;
import com.intellij.psi.stubs.IStubElementType;

public class GoRangeClauseImpl extends GoVarSpecImpl implements GoRangeClause {

  public GoRangeClauseImpl(ASTNode node) {
    super(node);
  }

  public GoRangeClauseImpl(com.goide.stubs.GoVarSpecStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public void accept(@NotNull GoVisitor visitor) {
    visitor.visitRangeClause(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoVisitor) accept((GoVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<GoExpression> getExpressionList() {
    return GoPsiTreeUtil.getChildrenOfTypeAsList(this, GoExpression.class);
  }

  @Override
  @NotNull
  public List<GoVarDefinition> getVarDefinitionList() {
    return GoPsiTreeUtil.getStubChildrenOfTypeAsList(this, GoVarDefinition.class);
  }

  @Override
  @Nullable
  public PsiElement getAssign() {
    return findChildByType(ASSIGN);
  }

  @Override
  @Nullable
  public PsiElement getVarAssign() {
    return findChildByType(VAR_ASSIGN);
  }

  @Override
  @NotNull
  public PsiElement getRange() {
    return notNullChild(findChildByType(RANGE));
  }

  @Nullable
  public GoExpression getRangeExpression() {
    return GoPsiImplUtil.getRangeExpression(this);
  }

  @NotNull
  public List<GoExpression> getLeftExpressionsList() {
    return GoPsiImplUtil.getLeftExpressionsList(this);
  }

  @NotNull
  public List<GoExpression> getRightExpressionsList() {
    return GoPsiImplUtil.getRightExpressionsList(this);
  }

}
