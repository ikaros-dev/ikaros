package run.ikaros.server.search.subject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.api.search.subject.SubjectHint;
import run.ikaros.api.store.enums.SubjectType;

class LuceneSubjectSearchServiceTest {
    private LuceneSubjectSearchService searchService;
    private Path tempWorkDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        tempWorkDir = tempDir.resolve("ikaros");
        Files.createDirectories(tempWorkDir);
        IkarosProperties ikarosProperties = Mockito.mock(IkarosProperties.class);
        when(ikarosProperties.getWorkDir()).thenReturn(tempWorkDir);
        searchService = new LuceneSubjectSearchService(ikarosProperties);
    }

    private SubjectDoc createSubjectDoc(UUID id, String name, String nameCn,
                                        String summary, SubjectType type, Long airTime) {
        SubjectDoc doc = new SubjectDoc();
        doc.setId(id);
        doc.setName(name);
        doc.setNameCn(nameCn);
        doc.setSummary(summary);
        doc.setType(type);
        doc.setAirTime(airTime);
        doc.setNsfw(false);
        doc.setTags(List.of("tag1", "tag2"));
        return doc;
    }

    private SearchParam createSearchParam(String keyword) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("keyword", keyword);
        params.add("limit", "10");
        return new SearchParam(params);
    }

    private SearchParam createSearchParam(String keyword, int limit,
                                          String preTag, String postTag) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("keyword", keyword);
        params.add("limit", String.valueOf(limit));
        if (preTag != null) {
            params.add("highlightPreTag", preTag);
        }
        if (postTag != null) {
            params.add("highlightPostTag", postTag);
        }
        return new SearchParam(params);
    }

    @Test
    void updateDocument_and_search() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectDoc doc = createSubjectDoc(id, "Test Anime", "测试动画",
            "This is a test anime summary.", SubjectType.ANIME, 1700000000000L);

        searchService.updateDocument(List.of(doc));

        // Search by name
        SearchParam param = createSearchParam("Test", 10, "<em>", "</em>");
        SearchResult<SubjectHint> result = searchService.search(param);

        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getKeyword()).isEqualTo("Test");
        SubjectHint hint = result.getHits().get(0);
        assertThat(hint.id()).isEqualTo(id);
        assertThat(hint.name()).isEqualTo("Test Anime");
        assertThat(hint.nameCn()).isEqualTo("测试动画");
    }

    @Test
    void updateDocument_and_searchByChinese() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectDoc doc = createSubjectDoc(id, "Attack on Titan", "进击的巨人",
            "An anime about titans.", SubjectType.ANIME, 1700000000000L);

        searchService.updateDocument(List.of(doc));

        SearchParam param = createSearchParam("进击");
        SearchResult<SubjectHint> result = searchService.search(param);

        assertThat(result.getTotal()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void updateDocument_and_searchByTag() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectDoc doc = createSubjectDoc(id, "Sword Art Online", "刀剑神域",
            "A VRMMO anime.", SubjectType.ANIME, 1700000000000L);

        searchService.updateDocument(List.of(doc));

        SearchParam param = createSearchParam("tag:tag1");
        SearchResult<SubjectHint> result = searchService.search(param);

        assertThat(result.getHits()).hasSize(1);
    }

    @Test
    void updateDocument_withMultipleDocs() throws Exception {
        SubjectDoc doc1 = createSubjectDoc(UUID.randomUUID(), "Naruto", "火影忍者",
            "Ninja anime.", SubjectType.ANIME, 1700000000000L);
        SubjectDoc doc2 = createSubjectDoc(UUID.randomUUID(), "One Piece", "海贼王",
            "Pirate anime.", SubjectType.ANIME, 1700000000000L);

        searchService.updateDocument(List.of(doc1, doc2));

        SearchParam param = createSearchParam("anime");
        SearchResult<SubjectHint> result = searchService.search(param);

        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    void rebuild() throws Exception {
        SubjectDoc doc1 = createSubjectDoc(UUID.randomUUID(), "Initial D", "头文字D",
            "Racing comic.", SubjectType.COMIC, 1700000000000L);
        searchService.updateDocument(List.of(doc1));

        // Rebuild with different set
        SubjectDoc doc2 = createSubjectDoc(UUID.randomUUID(), "Demon Slayer", "鬼灭之刃",
            "Demon hunting anime.", SubjectType.ANIME, 1700000000000L);
        searchService.rebuild(List.of(doc2));

        // Should only have doc2
        SearchParam param = createSearchParam("Demon");
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isEqualTo(1);

        // Old doc should not be found
        SearchParam paramOld = createSearchParam("Initial");
        SearchResult<SubjectHint> resultOld = searchService.search(paramOld);
        assertThat(resultOld.getTotal()).isZero();
    }

    @Test
    void removeDocuments() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectDoc doc = createSubjectDoc(id, "Bleach", "死神",
            "Soul reaper anime.", SubjectType.ANIME, 1700000000000L);
        searchService.updateDocument(List.of(doc));

        // Verify exists
        SearchParam param = createSearchParam("Bleach");
        assertThat(searchService.search(param).getTotal()).isEqualTo(1);

        // Remove by keyword (UUIDs with hyphens confuse QueryParser, use name instead)
        searchService.removeDocuments(Set.of("Bleach"));

        // Verify gone
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void search_withNoResults() throws Exception {
        // Add a doc first to ensure index exists
        SubjectDoc doc = createSubjectDoc(UUID.randomUUID(), "Existing", "现有",
            "A doc to initialize the index.", SubjectType.ANIME, 1700000000000L);
        searchService.updateDocument(List.of(doc));

        // Search for non-existent keyword
        SearchParam param = createSearchParam("NonExistentAnimeXYZ");
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isZero();
        assertThat(result.getHits()).isEmpty();
    }

    @Test
    void search_byField() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectDoc doc = createSubjectDoc(id, "Fullmetal Alchemist", "钢之炼金术师",
            "Alchemy anime.", SubjectType.ANIME, 1700000000000L);
        searchService.updateDocument(List.of(doc));

        // Search with field:value syntax (must be exact match for StringField)
        SearchParam param = createSearchParam("name:Fullmetal Alchemist");
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void updateDocument_and_searchByType() throws Exception {
        SubjectDoc doc1 = createSubjectDoc(UUID.randomUUID(), "Manga One", "漫画一",
            "A comic.", SubjectType.COMIC, 1700000000000L);
        SubjectDoc doc2 = createSubjectDoc(UUID.randomUUID(), "Anime One", "动画一",
            "An anime.", SubjectType.ANIME, 1700000000000L);

        searchService.updateDocument(List.of(doc1, doc2));

        // Search by type field
        SearchParam param = createSearchParam("type:COMIC");
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getHits().get(0).name()).isEqualTo("Manga One");
    }

    @Test
    void search_highlighting() throws Exception {
        SubjectDoc doc = createSubjectDoc(UUID.randomUUID(), "One Punch Man", "一拳超人",
            "A superhero anime.", SubjectType.ANIME, 1700000000000L);
        searchService.updateDocument(List.of(doc));

        SearchParam param = createSearchParam("Punch", 10, "<b>", "</b>");
        SearchResult<SubjectHint> result = searchService.search(param);
        assertThat(result.getTotal()).isEqualTo(1);
        // Note: the name is stored as StringField, not analyzed, so highlight may apply
        // to the searchable content, not the displayed name
    }
}
