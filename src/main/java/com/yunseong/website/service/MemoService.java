package com.yunseong.website.service;

import com.yunseong.website.domain.Memo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemoService {

    List<Memo> memos = new ArrayList<>();

    public MemoService() {
        saveMemo("README", """
                YunseongJeong
                =============
                ## About me
                I'm a developer who cares about well-structured code and good software design. I'm especially interested in building solid architecture by combining theory and hands-on experience.
                                
                ## Contacts
                - [Tistory](https://yunseong.tistory.com/)
                - [Gmail](mailto:dev.yunseong@gmail.com)
                                
                ## Projects & Experiences
                ### [워드 온라인](https://apptive-game-team.github.io/WordOnline_Play/) (2025.04 ~ )
                - 실시간 멀티플레이 카드 게임 서버를 Spring Boot로 개발
                ### [두도지](https://github.com/team-dudoji) (2025.01 ~ )
                - 도보 여행에 게임 요소를 접목한 안드로이드 앱 개발
                ### [Drug Master](https://github.com/orgs/drug-prometheus) (2024.09 ~ 2024.11)
                - React로 약물 정보 제공 및 복약 관리 프론트엔드 구현
                - Dev-ton 해커톤에서 혁신상 수상, UI/UX 경험 확대
                ### [경단](https://github.com/Gyeongdan) (2024.06 ~ 2024.07)
                - FastAPI로 백엔드 구현, 협업 필터링 기반 추천 시스템과 메일 발송 기능 개발
                - SW 중심대학 디지털 경진대회 본선 진출 성과 달성
                ### Awake (2024.05 ~ 2024.12)
                - 졸음 운전 방지용 LLM 챗봇 및 Flutter 앱 개발
                - 3개 해커톤에서 수상, Android 기술과 LLM, 음성 기반 인터페이스 경험
                                
                ## Awards
                - 2024 공공 빅데이터 활용 경진대회 - 장려상 수상
                - 제11회 대한민국 SW 융합 해커톤 - 우수상 수상
                - 제5회 PNU 창의 융합 해커톤 - 최우수상 수상
                - 2024 부산 디지털 혁신 아카데미 해커톤 - 혁신상 수상
                - 제4회 PNU Coding Challenge - 장려상 수상
                                
                ## Education
                - 부산대학교 정보컴퓨터공학부 (2021.03 ~ )
                - 카카오테크캠퍼스 3기 (2025.04 ~ )
                                
                ## Certificates
                                
                ![Yunseong's GitHub stats](https://github-readme-stats.vercel.app/api?username=dev-yunseong&show_icons=true&theme=dracula)
                """);
    }

    public long saveMemo(String title, String content) {
        Memo memo = new Memo((long) memos.size(), title, content);
        memos.add(memo);
        return memo.getId();
    }

    public Memo getMemo(long memoId) {
        return memos.get((int) memoId);
    }

    public void updateMemo(long memoId, String title, String content) {
        Memo memo = memos.get((int) memoId);
        memo.setTitle(title);
        memo.setContent(content);
    }
}
