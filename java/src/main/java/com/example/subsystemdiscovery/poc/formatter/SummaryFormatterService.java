package com.example.subsystemdiscovery.poc.formatter;

import java.util.Collections;
import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

@Service
public class SummaryFormatterService {

    public String format(String summary) {
        if (summary == null) {
            return "";
        }
        
        summary = cleanSummary(summary);

        List<Extension> extensions = Collections.singletonList(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

        return renderer.render(parser.parse(summary));
    }

    private String cleanSummary(String summary) {
        if (summary == null) {
            return "";
        }
        return summary
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("(?s)<thought>.*?</thought>", "")
                .replaceAll("B</thought>", "")
                .trim();
    }
}
