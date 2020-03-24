// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public final class LocalInspectionsPassFactory implements MainHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  private static final Logger LOG = Logger.getInstance(LocalInspectionsPassFactory.class);

  @Override
  public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
    int[] GHP = {Pass.UPDATE_ALL};
    boolean runInspectionsAfterCompletionOfGeneralHighlightPass =
      ((TextEditorHighlightingPassRegistrarImpl)registrar).isRunInspectionsAfterCompletionOfGeneralHighlightPass();
    registrar.registerTextEditorHighlightingPass(this, runInspectionsAfterCompletionOfGeneralHighlightPass ? GHP : null,
                                                 runInspectionsAfterCompletionOfGeneralHighlightPass ? null : GHP, true, Pass.LOCAL_INSPECTIONS);
  }

  @NotNull
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull final Editor editor) {
    TextRange textRange = FileStatusMap.getDirtyTextRange(editor, Pass.LOCAL_INSPECTIONS);
    if (textRange == null){
      return new ProgressableTextEditorHighlightingPass.EmptyPass(file.getProject(), editor.getDocument());
    }
    TextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new MyLocalInspectionsPass(file, editor.getDocument(), textRange, visibleRange, new DefaultHighlightInfoProcessor());
  }

  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@NotNull PsiFile file,
                                                               @NotNull Document document,
                                                               @NotNull HighlightInfoProcessor highlightInfoProcessor) {
    final TextRange textRange = file.getTextRange();
    LOG.assertTrue(textRange != null, "textRange is null for " + file + " (" + PsiUtilCore.getVirtualFile(file) + ")");
    return new MyLocalInspectionsPass(file, document, textRange, LocalInspectionsPass.EMPTY_PRIORITY_RANGE, highlightInfoProcessor);
  }

  private static class MyLocalInspectionsPass extends LocalInspectionsPass {
    private MyLocalInspectionsPass(@NotNull PsiFile file,
                                   Document document,
                                   @NotNull TextRange textRange,
                                   @NotNull TextRange visibleRange,
                                   @NotNull HighlightInfoProcessor highlightInfoProcessor) {
      super(file, document, textRange.getStartOffset(), textRange.getEndOffset(), visibleRange, true, highlightInfoProcessor, true);
    }

    @Override
    protected boolean isAcceptableLocalTool(@NotNull LocalInspectionToolWrapper wrapper) {
      return !wrapper.runForWholeFile();
    }
  }
}
