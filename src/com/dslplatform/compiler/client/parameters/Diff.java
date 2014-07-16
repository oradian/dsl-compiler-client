package com.dslplatform.compiler.client.parameters;

import com.dslplatform.compiler.client.CompileParameter;
import com.dslplatform.compiler.client.Context;
import com.dslplatform.compiler.client.InputParameter;
import com.dslplatform.compiler.client.diff.diff_match_patch;

import java.util.*;

public enum Diff implements CompileParameter {
	INSTANCE;

	private static void compareDsls(final Context context) {
		final Map<String, String> currentDsl = DslPath.getCurrentDsl(context);
		final Map<String, String> previousDsl = DbConnection.getDatabaseDsl(context);

		final Set<String> currentFiles = new HashSet<String>(currentDsl.keySet());
		currentFiles.removeAll(previousDsl.keySet());
		for (final String name : currentFiles) {
			context.log("New file: " + name + ". Total lines: " + currentDsl.get(name).split("\n").length);
			//TODO: options which control whether to show content
			//context.log("----------------------------------------------");
			//context.log(currentDsl.get(name));
		}
		final Set<String> previousFiles = new HashSet<String>(previousDsl.keySet());
		previousFiles.removeAll(currentDsl.keySet());
		for (final String name : previousFiles) {
			context.log("Removed file: " + name + ". Total lines: " + previousDsl.get(name).split("\n").length);
			//context.log("----------------------------------------------");
			//context.log(previousDsl.get(name));
		}
		final Set<String> sharedFiles = new HashSet<String>(currentDsl.keySet());
		sharedFiles.retainAll(previousDsl.keySet());
		diff_match_patch diff = new diff_match_patch();
		boolean hasChanges = false;
		for (final String name : sharedFiles) {
			String current = currentDsl.get(name);
			String previous = previousDsl.get(name);
			if (current.equals(previous)) {
				continue;
			}
			LinkedList<diff_match_patch.Diff> changes = diff.diff_main(previous, current);
			context.log("Changed file: " + name);
			context.log("----------------------------------------------");
			final int totalDiffs = changes.size();
			int cur = 0;
			hasChanges = hasChanges || totalDiffs > 0;
			final StringBuilder sb = new StringBuilder();
			for (final diff_match_patch.Diff aDiff : changes) {
				cur++;
				final String text = aDiff.text;
				switch (aDiff.operation) {
					case INSERT:
						sb.append("[+ ").append(text).append("]");
						break;
					case DELETE:
						sb.append("[- ").append(text).append("]");
						break;
					case EQUAL:
						String[] lines = text.split("\n");
						if (cur < totalDiffs) {
							if (lines.length <= 10) {
								sb.append(text);
							} else {
								int width = 0;
								if (cur > 1) {
									for (int i = 0; i < 5; i++) {
										width += lines[i].length() + 1;
									}
									sb.append(text.substring(0, width));
									width = 0;
								}
								for (int i = Math.max(5, lines.length - 5); i < lines.length; i++) {
									width += lines[i].length() + 1;
								}
								sb.append("\n").append("...").append("\n");
								sb.append(text.substring(text.length() - width));
							}
						} else {
							if (lines.length <= 5) {
								sb.append(text);
							}
							int width = 0;
							for (int i = 0; i < 5; i++) {
								width += lines[i].length() + 1;
							}
							sb.append(text.substring(0, width));
						}
						break;
				}
			}
			context.log(sb.toString());
			context.log();
		}
		if(currentFiles.size() == 0 && previousFiles.size() == 0 && !hasChanges) {
			context.log("No changes found in DSL");
		}
	}

	@Override
	public boolean check(final Context context) {
		if (context.contains(InputParameter.DIFF)) {
			if (!context.contains(InputParameter.CONNECTION_STRING)) {
				context.error("Connection string is required to perform a diff operation");
				System.exit(0);
			}
		}
		return true;
	}

	@Override
	public void run(final Context context) {
		if (context.contains(InputParameter.DIFF)) {
			compareDsls(context);
		}
	}

	@Override
	public String getShortDescription() {
		return "Diff current DSL files to previous DSL files";
	}

	@Override
	public String getDetailedDescription() {
		return "Provide diff on changed DSL files. For fast confirmation of changes done before creating new library models or SQL migrations.\n" +
				"Diff requires read-only access to -NGS- schema in Postgres database where previously applied DSL is stored.\n" +
				"To disable diff confirmation, use no prompt parameter.";
	}
}
