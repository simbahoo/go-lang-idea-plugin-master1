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

package com.goide.psi.impl;

import com.goide.psi.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoFieldNameReference extends GoCachedReference<GoReferenceExpressionBase> {
  private GoCompositeElement myValue;

  public GoFieldNameReference(@NotNull GoReferenceExpressionBase element) {
    super(element);
    GoCompositeElement place = myElement;
    while ((place = PsiTreeUtil.getParentOfType(place, GoLiteralValue.class)) != null) {
      if (place.getParent() instanceof GoValue) {
        myValue = (GoValue)place.getParent();
        break;
      }
    }
  }

  @Override
  public boolean processResolveVariants(@NotNull final GoScopeProcessor processor) {
    GoScopeProcessor fieldProcessor = processor instanceof GoFieldProcessor ? processor : new GoFieldProcessor(myElement) {
      @Override
      public boolean execute(@NotNull PsiElement e, @NotNull ResolveState state) {
        return super.execute(e, state) && processor.execute(e, state);
      }
    };
    GoKey key = PsiTreeUtil.getParentOfType(myElement, GoKey.class);
    GoValue value = PsiTreeUtil.getParentOfType(myElement, GoValue.class);
    if (key == null && (value == null || PsiTreeUtil.getPrevSiblingOfType(value, GoKey.class) != null)) return true;

    GoCompositeLit lit = getLiteral();
    GoType type = GoPsiImplUtil.getLiteralType(lit);
    
    PsiElement p = PsiTreeUtil.getParentOfType(myElement, GoLiteralValue.class);
    while (lit != null && p != null) {
      if (p == lit) break;
      if (p instanceof GoLiteralValue) type = getType(type);
      p = p.getParent();
    }

    if (!processStructType(fieldProcessor, type)) return false;
    return !(type instanceof GoPointerType && !processStructType(fieldProcessor, ((GoPointerType)type).getType()));
  }

  @Nullable
  private GoCompositeLit getLiteral() {return PsiTreeUtil.getParentOfType(myElement, GoCompositeLit.class);}

  private boolean processStructType(@NotNull GoScopeProcessor fieldProcessor, @Nullable GoType type) {
    return !(type instanceof GoStructType && !type.processDeclarations(fieldProcessor, ResolveState.initial(), null, myElement));
  }

  @Nullable
  private GoType getType(@Nullable GoType type) { // todo: rethink and unify this algorithm
    boolean inValue = myValue != null;
    
    if (inValue && type instanceof GoArrayOrSliceType) {
      type = ((GoArrayOrSliceType)type).getType();
    }
    else if (type instanceof GoMapType) {
      type = inValue ? ((GoMapType)type).getValueType() : ((GoMapType)type).getKeyType();
    }
    else if (inValue && type instanceof GoSpecType) {
      GoType inner = ((GoSpecType)type).getType();
      if (inner instanceof GoArrayOrSliceType) {
        type = ((GoArrayOrSliceType)inner).getType();
      }
      else if (inner instanceof GoStructType) {
        GoKey key = PsiTreeUtil.getPrevSiblingOfType(myValue, GoKey.class);
        GoFieldName field = key != null ? key.getFieldName() : null;
        PsiElement resolve = field != null ? field.resolve() : null;
        if (resolve instanceof GoFieldDefinition) {
          type = PsiTreeUtil.getNextSiblingOfType(resolve, GoType.class);
        }
      }
    }

    if (type != null && type.getTypeReferenceExpression() != null) {
      type = type.getUnderlyingType();
    }

    if (type instanceof GoPointerType) {
      GoType inner = ((GoPointerType)type).getType();
      if (inner != null && inner.getTypeReferenceExpression() != null) {
        type = inner.getUnderlyingType();
      }
    }

    return type instanceof GoSpecType ? ((GoSpecType)type).getType() : type;
  }

  public boolean inStructTypeKey() {
    return myValue == null && GoPsiImplUtil.getLiteralType(getLiteral()) instanceof GoStructType;
  }

  @Nullable
  @Override
  public PsiElement resolveInner() {
    GoScopeProcessorBase p = new GoFieldProcessor(myElement);
    processResolveVariants(p);
    return p.getResult();
  }

  private static class GoFieldProcessor extends GoScopeProcessorBase {
    private final Module myModule;

    public GoFieldProcessor(@NotNull PsiElement element) {
      super(element);
      PsiFile containingFile = myOrigin.getContainingFile();
      myModule = containingFile != null ? ModuleUtilCore.findModuleForPsiElement(containingFile.getOriginalFile()) : null;
    }

    @Override
    protected boolean crossOff(@NotNull PsiElement e) {
      if (!(e instanceof GoFieldDefinition) && !(e instanceof GoAnonymousFieldDefinition)) return true;
      GoNamedElement named = (GoNamedElement)e;
      PsiFile myFile = myOrigin.getContainingFile();
      PsiFile file = e.getContainingFile();
      if (!(myFile instanceof GoFile) || !GoPsiImplUtil.allowed(file, myFile, myModule)) return true;
      boolean localResolve = GoReference.isLocalResolve(myFile, file);
      return !e.isValid() || !(named.isPublic() || localResolve);
    }
  }
}
