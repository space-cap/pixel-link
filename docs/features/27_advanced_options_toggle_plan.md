# 고급 링크 5대 기능 개별 ON/OFF 제어 구현 계획서

본 문서는 관리자가 시스템 설정(Admin Settings) 페이지에서 5가지 고급 기능(맞춤형 단축 경로, 소셜 프리뷰/SEO, 스마트 라우팅, 수익화, 마케팅 픽셀)을 각각의 체크박스로 개별 제어하여, 사용자가 보는 고급 링크 생성 페이지(`/app/create`)의 기능 노출을 독립적으로 보이고 숨길 수 있는 관리 시스템의 구현 계획을 정리한 문서입니다.

---

## 1. 개요 및 목적

- **현 상태 및 요구사항**:
  - 기존에는 고급 링크 생성 페이지에 항상 5가지 아코디언 메뉴가 고정 노출되어 있어, 특정 기능을 서비스하지 않거나 제한하고자 할 때 유연하게 대응하기 어렵습니다.
  - 이에 따라 관리자 시스템 설정 화면에 **5가지 기능 각각에 대응하는 5개의 체크박스**를 신설합니다.
  - 관리자가 원하는 기능만 개별적으로 켜고 끌 수 있게 하여, 사용자가 접속하는 고급 링크 생성 화면에는 활성화된 옵션 카드(아코디언 섹션)만 동적으로 렌더링되도록 통제합니다.
  - 특히 **5가지 모든 기능이 비활성화(OFF)된 상태**이더라도 페이지 접근을 전면 차단하여 대시보드로 튕겨버리는 대신, **기본 기능인 단축 URL 생성 입력창(Destination URL)과 생성 버튼만 노출되는 초간결한 폼으로 페이지가 자연스럽게 동작**하도록 설계하여 사용자가 기본 단축 URL 링크 발급 기능을 정상적으로 이용할 수 있게 유연성을 보장합니다.

---

## 2. 상세 구현 방안

### 2.1. 데이터베이스(DB) 및 서비스 아키텍처 설계

- **설정 키 세분화 정의**:
  - `system_settings` 테이블에 5대 고급 기능 각각의 활성화를 통제할 신규 설정 키를 주입합니다 (기본값은 모두 `'true'`로 설정).
    1. `feature_custom_slug_enabled` : 맞춤형 단축 경로 (Custom Slug)
    2. `feature_seo_preview_enabled` : 소셜 프리뷰 및 SEO
    3. `feature_smart_routing_enabled` : 스마트 라우팅 (기기/OS 분기)
    4. `feature_monetization_enabled` : 수익화 (페이월 잠금 & 광고)
    5. `feature_marketing_pixel_enabled` : 마케팅 픽셀 및 트래킹 스크립트

- **공통 뷰 모델 데이터 바인딩 (@ControllerAdvice 도입)**:
  - 5개의 설정값이 거의 모든 화면(대시보드, 링크 관리 등)의 공통 레이아웃인 **사이드바(Sidebar)** 및 본문에서 참조되어야 하므로, 개별 컨트롤러 핸들러마다 일일이 바인딩 코드를 작성하는 대신 **`@ControllerAdvice`**를 신설하여 공통 모델 속성을 주입합니다.
  - 이를 통해 코드 중복을 완벽히 제거하고 후임자도 쉽게 유지보수할 수 있는 스프링 친화적 아키텍처를 구현합니다.

```java
@ControllerAdvice(assignableTypes = {DashboardController.class})
public class GlobalModelAdvice {

    @Autowired
    private LinkService linkService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("customSlugEnabled", "true".equals(linkService.getSystemSetting("feature_custom_slug_enabled", "true")));
        model.addAttribute("seoPreviewEnabled", "true".equals(linkService.getSystemSetting("feature_seo_preview_enabled", "true")));
        model.addAttribute("smartRoutingEnabled", "true".equals(linkService.getSystemSetting("feature_smart_routing_enabled", "true")));
        model.addAttribute("monetizationEnabled", "true".equals(linkService.getSystemSetting("feature_monetization_enabled", "true")));
        model.addAttribute("marketingPixelEnabled", "true".equals(linkService.getSystemSetting("feature_marketing_pixel_enabled", "true")));
    }
}
```

---

### 2.2. 관리자 설정 화면 UI 구현

#### [MODIFY] [admin/settings.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/admin/settings.html)
- 관리자 설정 페이지의 시스템 설정 폼 내부에 "고급 기능 개별 활성화 제어" 카드 영역을 신설하고, 5가지 기능에 대응하는 개별 체크박스 필드를 배치합니다.

**마크업 예시**:
```html
<section class="card-mini" style="margin-top: 24px; padding: 20px; background: rgba(255,255,255,0.01); border: 1px solid var(--card-border); border-radius: 12px;">
    <h4 style="margin-bottom: 15px; font-weight: 700; color: var(--text-main);">⚙️ 고급 링크 기능 활성화 제어</h4>
    <div style="display: flex; flex-direction: column; gap: 12px;">
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" name="customSlugEnabled" th:checked="${customSlugEnabled}" style="width: 16px; height: 16px;">
            <span>1. 맞춤형 단축 경로 설정 (Custom Slug) 노출</span>
        </label>
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" name="seoPreviewEnabled" th:checked="${seoPreviewEnabled}" style="width: 16px; height: 16px;">
            <span>2. 소셜 프리뷰 및 SEO 설정 노출</span>
        </label>
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" name="smartRoutingEnabled" th:checked="${smartRoutingEnabled}" style="width: 16px; height: 16px;">
            <span>3. 기기 및 OS 기반 스마트 라우팅 노출</span>
        </label>
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" name="monetizationEnabled" th:checked="${monetizationEnabled}" style="width: 16px; height: 16px;">
            <span>4. 수익화 (페이월 잠금 & 광고 노출) 노출</span>
        </label>
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="checkbox" name="marketingPixelEnabled" th:checked="${marketingPixelEnabled}" style="width: 16px; height: 16px;">
            <span>5. 마케팅 픽셀 및 스크립트 트래킹 노출</span>
        </label>
    </div>
</section>
```

- 저장 처리 시, 폼 파라미터로 넘어온 5개 체크박스 값이 `LinkService`를 통해 5개 설정 키에 각각 갱신 저장되도록 서버 액션 로직을 바인딩합니다.

---

### 2.3. 고급 생성 화면 및 사이드바 동적 제어 설계

#### 1) 고급 링크 생성 페이지 아코디언 노출 제어
#### [MODIFY] [create.html](file:///c:/workdir/space-cap/pixel-link/src/main/resources/templates/create.html)
- 5개 아코디언 섹션(`.accordion-section`) 각각에 Thymeleaf `th:if` 속성을 부여하여, 활성화된 기능만 화면에 렌더링되게 처리합니다.
- 만약 5가지 기능이 모두 비활성화된 경우, 아코디언 컨테이너 전체(`.accordion-container`)는 화면에 아예 렌더링되지 않으며, 사용자에게는 기본 기능인 `Destination URL` 입력란과 생성 버튼만 노출되어 **심플한 기본 단축 링크 생성 폼**으로 완벽하게 기능하게 됩니다.

```html
<!-- 5대 고급 옵션 아코디언 그룹 (활성화된 기능이 1개라도 있는 경우에만 컨테이너 노출) -->
<div class="accordion-container" style="margin-bottom: 28px;"
     th:if="${customSlugEnabled || seoPreviewEnabled || smartRoutingEnabled || monetizationEnabled || marketingPixelEnabled}">
    
    <!-- Section 1: Custom Slug -->
    <div class="accordion-section" th:if="${customSlugEnabled}">
        ...
    </div>

    <!-- Section 2: SEO Social Share Preview -->
    <div class="accordion-section" th:if="${seoPreviewEnabled}">
        ...
    </div>

    <!-- Section 3: Smart Routing -->
    <div class="accordion-section" th:if="${smartRoutingEnabled}">
        ...
    </div>

    <!-- Section 4: Monetization -->
    <div class="accordion-section" th:if="${monetizationEnabled}">
        ...
    </div>

    <!-- Section 5: Pixels & Tracking Scripts -->
    <div class="accordion-section" th:if="${marketingPixelEnabled}">
        ...
    </div>
</div>
```

#### 2) 접근 허용 및 서비스 흐름 유지
- 이전 설계와 달리 5대 고급 옵션이 모두 비활성화(OFF)되어도 사용자의 진입을 차단하거나 리다이렉트 시키지 않고 `/app/create` 접근을 온전히 허용합니다.
- 사이드바 메뉴 명칭인 **"고급 링크 생성"**은 그대로 유지하여 진입 루트를 제공하며, 유저가 페이지 내에 들어왔을 때 자동으로 초간결 기본 폼으로 스위칭되게 설계합니다.

---

## 3. 검증 계획

### 3.1. 수동 검증 항목
1. **5대 옵션 전체 비활성화 시 기본 단축 기능 동작**:
   - 관리자 설정에서 5대 고급 기능을 모두 체크 해제(OFF)하고 저장합니다.
   - 일반 유저로 로그인 후 `/app/create` 링크 생성 페이지에 진입합니다.
   - 아코디언 그룹 전체가 보이지 않고 **목적지 URL 입력란과 생성하기 버튼만 있는 기본 폼**이 단정하게 출력되는지 확인합니다.
   - URL 입력 후 단축 링크를 성공적으로 생성하여 기본 기능이 완벽하게 작동하는지 검증합니다.
2. **일부 활성화 시 동적 스위칭**:
   - 특정 옵션들만 켰을 때(ON), 해당 옵션 아코디언만 활성화되어 화면에 알맞게 정렬 노출되는지 확인합니다.
3. **각 기능 독립 제어 및 데이터 검증**:
   - 체크 해제된 항목의 값은 API 전송 시 전달되지 않거나 기본값으로 매핑되어 저장되는지 검증합니다.
