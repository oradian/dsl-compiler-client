package com.dslplatform.ideaplugin;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class DslSyntaxHighlighter extends SyntaxHighlighterBase {
	private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

	private final Project project;
	private final VirtualFile virtualFile;

	public DslSyntaxHighlighter(Project project, VirtualFile virtualFile) {
		this.project = project;
		this.virtualFile = virtualFile;
	}

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		return new DslLexerParser(project, virtualFile);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		if (tokenType instanceof TokenType) {
			TokenType tt = (TokenType) tokenType;
			return tt.attributes;
		}
		return EMPTY_KEYS;
	}
}