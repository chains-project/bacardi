# Comparing fixes from [BUMP](https://github.com/chains-project/bump/blob/main/fixes) with [BUMPER](https://github.com/chains-project/bumper/tree/main/results/benchmark/20240524_with_diff/bump/advanced/gpt4) and japicmp

## Breaking commit: 0abf7148300f40a1da0538ab060552bca4a2f1d8

```patch
diff --git a/src/main/java/xdev/tableexport/export/ReportBuilder.java b/src/main/java/xdev/tableexport/export/ReportBuilder.java
index 4e91b33..c57c796 100644
--- a/src/main/java/xdev/tableexport/export/ReportBuilder.java
+++ b/src/main/java/xdev/tableexport/export/ReportBuilder.java
@@ -366,7 +366,7 @@ public class ReportBuilder
 			return;
 		}
 		
-		textField.getLineBox().getPen().setLineWidth(border.getLineWidth());
+		textField.getLineBox().getPen().setLineWidth(Float.valueOf(border.getLineWidth()));
 		textField.getLineBox().getPen().setLineColor(border.getLineColor());
 		textField.getLineBox().getPen().setLineStyle(border.getLineStyle().getLineStyleEnum());
 	}
```    



## Breaking Commit: b8f92ff37d1aed054d8320283fd6d6a492703a55

