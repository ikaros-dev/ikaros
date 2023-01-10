package run.ikaros.server.core.warp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author: li-guohao
 */
class PagingWrapTest {

    @BeforeEach
    void setUp() {
        ArrayList<String> strList = new ArrayList<>();
        strList.add("1");
        strList.add("2");
        strList.add("3");
        strList.add("4");
        PagingWrap<String> pagingWrap = new PagingWrap<>(strList);
    }

    @Test
    void isFirstPage() {
        PagingWrap<String> pagingWrap = new PagingWrap<>(List.of("1", "2", "3", "4"));
        assertThat(pagingWrap.isFirstPage()).isTrue();

        pagingWrap = new PagingWrap<>(2, 4, 30L, List.of("1", "4", "324", "ssadf"));
        assertThat(pagingWrap.isFirstPage()).isFalse();
    }

    @Test
    void isLastPage() {
        // page < 0
        PagingWrap<String> pagingWrap = new PagingWrap<>(-1, 2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.isLastPage()).isTrue();

        // page < totalPages
        pagingWrap = new PagingWrap<>(1, 2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.isLastPage()).isFalse();

        // page > totalPages
        pagingWrap = new PagingWrap<>(16, 2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.isLastPage()).isTrue();
    }

    @Test
    void hasNext() {
        // page < 0
        PagingWrap<String> pagingWrap = new PagingWrap<>(-1, -2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.hasNext()).isFalse();

        // page < totalPages
        pagingWrap = new PagingWrap<>(1, 2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.hasNext()).isTrue();

        // page > totalPages
        pagingWrap = new PagingWrap<>(16, 2, 30L, List.of("1", "3"));
        assertThat(pagingWrap.hasNext()).isFalse();
    }

    @Test
    void hasPrevious() {
        PagingWrap<String> pagingWrap = new PagingWrap<>(List.of("1", "2", "3", "4"));
        assertThat(pagingWrap.hasPrevious()).isFalse();

        pagingWrap = new PagingWrap<>(2, 4, 30L, List.of("1", "4", "324", "ssadf"));
        assertThat(pagingWrap.hasPrevious()).isTrue();
    }

    @Test
    void iterator() {
        PagingWrap<String> pagingWrap = new PagingWrap<String>(List.of("1", "2", "3", "4"));
        assertThat(pagingWrap.iterator())
            .isInstanceOf(Iterator.class);
        assertThat(pagingWrap.iterator().next())
            .isEqualTo("1");
    }

    @Test
    void isEmpty() {
        PagingWrap<String> pagingWrap = new PagingWrap<String>(List.of("1", "2", "3", "4"));
        assertThat(pagingWrap.isEmpty()).isFalse();
        pagingWrap = new PagingWrap<>(List.of());
        assertThat(pagingWrap.isEmpty()).isTrue();
    }

    @Test
    void getTotalPages() {
        PagingWrap<String> pagingWrap =
            new PagingWrap<String>(1, 4, 10, List.of("1", "2", "3", "4"));
        assertThat(pagingWrap.getTotalPages()).isEqualTo(3);
    }

    @Test
    void emptyResult() {
        assertThat(PagingWrap.emptyResult().isEmpty()).isTrue();
    }

    @Test
    void getPageAndSizeAndTotalAndItems() {
        List<String> strList = List.of("1", "2", "3", "4");
        PagingWrap<String> pagingWrap = new PagingWrap<String>(1, 4, 10, strList);
        assertThat(pagingWrap.getPage()).isEqualTo(1);
        assertThat(pagingWrap.getSize()).isEqualTo(4);
        assertThat(pagingWrap.getTotal()).isEqualTo(10);
        assertThat(pagingWrap.getItems()).isEqualTo(strList);
    }


    @Test
    void equals() {
        List<String> strList = List.of("1", "2", "3", "4");
        PagingWrap<String> pagingWrap = new PagingWrap<String>(1, 4, 10, strList);
        assertThat(pagingWrap.canEqual(new Object())).isFalse();
        assertThat(pagingWrap.equals(PagingWrap.emptyResult())).isFalse();
        assertThat(pagingWrap.equals(new PagingWrap<String>(1, 4, 10, strList))).isTrue();
        assertThat(pagingWrap.equals(new PagingWrap<String>(1, 5, 10, strList))).isFalse();
    }

    @Test
    void testIllegalArg() {
        PagingWrap<Object> objects = new PagingWrap<>(-1, -2, 40L, null);
        assertThat(objects.getItems()).isEqualTo(Collections.emptyList());
    }
}