package run.ikaros.server.tripartite.bgmtv.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.service.OptionServiceImpl;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.unittest.common.UnitTestConst;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static run.ikaros.server.unittest.MemberMatcher.field;

class BgmTvRepositoryImplTest {

    RestTemplate restTemplate;
    OptionService optionService;
    BgmTvRepositoryImpl bgmTvRepository;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        optionService = Mockito.mock(OptionServiceImpl.class);
        bgmTvRepository = new BgmTvRepositoryImpl(optionService);
    }

    @Test
    void setHttpHeaderUserAgent() throws NoSuchFieldException, IllegalAccessException {
        final String exceptUserAgent = "ikaros-dev/ikaros (https://github.com/ikaros-dev/ikaros)";

        Field headers = field(BgmTvRepositoryImpl.class, "headers");
        Object header = headers.get(bgmTvRepository);
        assertThat(header).isInstanceOf(HttpHeaders.class);

        HttpHeaders httpHeaders = (HttpHeaders) header;
        List<String> userAgentList = httpHeaders.get(HttpHeaders.USER_AGENT);
        assertThat(userAgentList).isNotEmpty();
        assertThat(userAgentList).contains(exceptUserAgent);
    }

    @Test
    void getSubject() throws NoSuchFieldException, IllegalAccessException {
        Long subjectId = 328609L;
        BgmTvSubject mockSubject = spy(new BgmTvSubject());
        ResponseEntity<BgmTvSubject> responseEntity = spy(new ResponseEntity<>(HttpStatus.OK));
        field(HttpEntity.class, "body").set(responseEntity, mockSubject);

        doReturn(responseEntity).when(restTemplate)
            .exchange(Mockito.anyString(), eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
                eq(BgmTvSubject.class));

        BgmTvSubject subject = bgmTvRepository.getSubject(subjectId);
        assertThat(subject).isNotNull();
        assertThat(subject).isEqualTo(mockSubject);

        verify(restTemplate).exchange(Mockito.anyString(), eq(HttpMethod.GET),
            Mockito.any(HttpEntity.class), eq(BgmTvSubject.class));
        verify(responseEntity).getBody();
    }

    @Test
    void getSubjectWhenNotFound() throws NoSuchFieldException, IllegalAccessException {
        Long subjectId = 328609L;

        HttpClientErrorException httpClientErrorException =
            new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpClientErrorException).when(restTemplate)
            .exchange(Mockito.anyString(), eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
                eq(BgmTvSubject.class));

        BgmTvSubject subject = bgmTvRepository.getSubject(subjectId);
        assertThat(subject).isNull();

        verify(restTemplate).exchange(Mockito.anyString(), eq(HttpMethod.GET),
            Mockito.any(HttpEntity.class), eq(BgmTvSubject.class));
    }

    @Test
    void getSubjectWhenOtherHttpException() {
        Long subjectId = 328609L;

        HttpClientErrorException exceptHttpClientErrorException =
            new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(exceptHttpClientErrorException).when(restTemplate)
            .exchange(Mockito.anyString(), eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
                eq(BgmTvSubject.class));

        try {
            bgmTvRepository.getSubject(subjectId);
            fail(UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (HttpClientErrorException exception) {
            assertThat(exception).isEqualTo(exceptHttpClientErrorException);
        }

        verify(restTemplate).exchange(Mockito.anyString(), eq(HttpMethod.GET),
            Mockito.any(HttpEntity.class), eq(BgmTvSubject.class));
    }

    //        @Test
    //    void searchSubjectWithNextApi() {
    //        String keyword = "air";
    //        BgmTvPagingData<BgmTvSubject> bgmTvPagingData =
    //            bgmTvRepository.searchSubjectWithNextApi(keyword);
    //        assertThat(bgmTvPagingData.getTotal()).isPositive();
    //        assertThat(bgmTvPagingData.getData()).isNotEmpty();
    //    }

    //        @Test
    void searchSubjectWithOldApi() {
        String keyword = "孤独摇滚";
        List<BgmTvSubject> bgmTvSubjects =
            bgmTvRepository.searchSubjectWithOldApi(keyword, BgmTvSubjectType.ANIME);
        assertThat(bgmTvSubjects).isNotEmpty();
    }
}