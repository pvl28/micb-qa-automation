package md.micb.qa.pages;

import org.openqa.selenium.By;

import java.util.List;

/** Any regular WordPress page of the site (product, "Despre bancă", etc.). */
public class ContentPage extends BasePage {

    public ContentPage open(String path) {
        openPath(path);
        return this;
    }

    /**
     * Visible page heading. The theme does not always use h1: product pages and
     * simple pages put the title into h1/h2/h3 inside the first section.
     */
    public String heading() {
        return wait.withMessage("No visible page heading found")
                .until(d -> {
                    List<By> candidates = List.of(
                            By.cssSelector("#wrapper h1"),
                            By.cssSelector("#wrapper section h2"),
                            By.cssSelector("#wrapper section.page-title h3"),
                            By.cssSelector("#wrapper section h3"));
                    for (By by : candidates) {
                        for (var el : d.findElements(by)) {
                            if (el.isDisplayed() && !el.getText().isBlank()) {
                                return el.getText().trim();
                            }
                        }
                    }
                    return null;
                });
    }

    /** Last breadcrumb item (Yoast SEO markup: .breadcrumb_last) = name of the current page. */
    public String currentBreadcrumb() {
        return textContent(visible(By.cssSelector(".breadcrumbs .breadcrumb_last")));
    }
}
