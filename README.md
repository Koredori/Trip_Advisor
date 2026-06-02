# 순천향대학교 2026-1학기 모바일프로그래밍 기말 프로젝트
**** 

## 1. 프로젝트 개요

사용자가 다녀온 여행기를 기록,조회,수정,삭제 할 수 있으며 사진과 메모를 함께  
관리하는 여행 계획 및 기록 앱을 제작하는 것이다.

> 제출 방식: .apk파일 , GitHub Repository URL
>
> 제출 기한: 2026년 6월 15일

****

## 2. 프로젝트 요구사항


SDK버전: **API Level 26 (Android Oreo ⬆️)**

### 2.1. 필수구현 사항

-----

아래 항목은 기말프로젝트의 필수 구현 사항이다.


1. **Fragment 최소 2개 이상 사용(백스택 관리 포함)**
> 홈화면에서, 2개 이상 Fragment로 화면 구성.
>
> BottomNavigationView 또는 버튼을 통하여 Fragment 전환 구현

2.**RecyclerView 활용, 클릭 이벤트 처리**

> Adapter와 ViewHolder를 구현하여 여행기록들의 목록을 표시한다.
>
> - 여행지명, 날짜, 대표사진(또는 썸네일)
> - 기록이 추가되고 삭제되어야 하며, DB와 연동 필요
> - 항목(여행기록) 클릭 시 해당 기록의 상세화면(사진, 메모, 지도 위치 등)으로 이동
> - 기록 추가/수정은 별도의 Activity로 분리하여 구현

3. **SQLite, CRUD(Create, Read, Update, Delete) 전체 동작**

> SQLiteOpenHelper를 상속한 DBHelper 클래스를 구현한다.
>
> - 여행기록 CRUD를 모두 SQLite로 처리
> - 앱을 종료해도 데이터가 그대로 유지

4. **옵션 메뉴 항목 2개 이상, 컨텍스트 메뉴 구현**
> - 전체삭제, 정렬기준 변경 등 옵션 메뉴 항목 2개 이상
> - 목록 항목을 길게 눌러 '수정', '삭제' 등의 컨텍스트 메뉴 1개 이상

5. **카메라 또는 갤러리 선택 Intent 구현**
> 사진을 기록할 수 있어야 하며, 상세화면에서 선택 사진이 보이도록 구현

6. **기본 예외처리 필수**

### 2.2. 선택구현 사항
___

아래 항목은 선택 구현 사항이다.

1. 지도 API 활용
   - 단순구현 (앱 상에 단순히 지도만 띄우는 것)
   - 사진 GPS추출 + 지도마커 생성

2. 스레드 및 코루틴 활용

3. 그 외 추가하고 싶은 기능들


---

## 3. 체크리스트

###  3.1 2개 이상의 Fragment를 사용했는가? ✅
- **구현 위치:** `MainActivity.kt`, `HomeFragment.kt`, `TripDetailFragment.kt`
- **상세 내용:**
  - `MainActivity.kt`에서 `BottomNavigationView`를 사용하여 탭 전환 및 초기 화면으로 `HomeFragment`를 로드
  - 여행 목록 화면(`HomeFragment`)과 상세 보기 화면(`TripDetailFragment`)으로 화면을 분할 구성
  - `HomeFragment`에서 목록 아이템을 클릭하면 `parentFragmentManager.beginTransaction().replace(...).addToBackStack(null).commit()`을 통해 상세 화면으로 전환하며 백스택에 추가
  - `TripDetailFragment`에서 뒤로가기 버튼(`btnBack`) 클릭 시 `popBackStack()`을 호출하여 백스택 관리를 수행
---
### 3.2 RecyclerView를 활용하고, 클릭 이벤트를 처리했는가? ✅
- **구현 위치:** `TripAdapter.kt`, `HomeFragment.kt`, `DetailThumbnailsAdapter` (in `TripDetailFragment.kt`)
- **상세 내용:**
  - `TripAdapter`와 `TripViewHolder`를 구현하여 전체 여행 기록 목록(여행지명, 날짜, 별점, 대표 이미지 썸네일)을 깔끔한 카드 레이아웃으로 표시
  - `HomeFragment`에서 리사이클러뷰의 각 아이템 클릭 이벤트(`onItemClick`)를 등록하여 항목 클릭 시 해당 기록의 상세 Fragment(`TripDetailFragment`)로 전환
  - 여행 기록 추가 및 수정은 별도의 독립 액티비티인 `AddTripActivity.kt`를 호출하여 처리
  - `TripDetailFragment` 내에서도 추가된 여러 사진들을 가로 스크롤 형태의 RecyclerView(`DetailThumbnailsAdapter`)로 표시하고 썸네일 클릭 시 메인 이미지가 바뀌는 클릭 이벤트를 제공
---
### 3.3 SQlite를 활용하고 CRUD가 정상적으로 동작하는가? ✅
- **구현 위치:** `TripDBHelper.kt`, `Trip.kt`
- **상세 내용:**
  - `SQLiteOpenHelper`를 상속받은 `TripDBHelper`를 통해 `trips`(여행지 정보) 및 `trip_images`(다중 이미지 경로) 테이블을 생성하고 연동
  - **Create (생성):** `insertTrip(trip)` 및 `saveImagesForTrip(tripId, images)`을 통해 신규 여행 기록 및 사진 정보를 데이터베이스에 삽입
  - **Read (조회):** `getAllTrips()`를 통해 전체 목록을 불러오고, `getTripById(id)`를 통해 특정 기록을 상세 조회
  - **Update (수정):** `updateTrip(trip)`을 통해 변경된 내용(제목, 장소, 날짜, 별점, 메모 등)을 데이터베이스에 업데이트
  - **Delete (삭제):** `deleteTrip(id)`로 개별 삭제(연동된 이미지 데이터도 Cascade 방식으로 자동 삭제)하며, `deleteAllTrips()`로 전체 삭제 지원
  - 앱을 완전히 종료했다가 다시 켜도 SQLite DB 파일에 저장되어 데이터가 안전하게 유지
---
### 3.4 옵션 메뉴 2개 이상, 컨텍스트 메뉴 1개 이상이 구현되어있는가? ✅
- **구현 위치:** `HomeFragment.kt`, `TripAdapter.kt`, `res/menu/home_menu.xml`
- **상세 내용:**
  - **옵션 메뉴:** `HomeFragment`의 우측 상단 툴바에 옵션 메뉴를 연동하여 1) 날짜순 정렬(`action_sort_date`), 2) 평점순 정렬(`action_sort_rating`), 3) 전체 삭제(`action_delete_all`)의 총 3개 메뉴 구현
  - **컨텍스트 메뉴:** `TripAdapter`의 `onBindViewHolder`에서 각 리스트 아이템에 `setOnCreateContextMenuListener`를 구현. 아이템을 길게 누르면 컨텍스트 메뉴("수정", "삭제")가 팝업되어 해당 항목에 대한 즉각적인 편집 및 삭제 조작이 가능
---
### 3.5 카메라 또는 갤러리 선택 Intent가 구현되어 있는가? ✅
- **구현 위치:** `AddTripActivity.kt`, `TripDetailFragment.kt`
- **상세 내용:**
  - `AddTripActivity`에서 이미지 추가 버튼 클릭 시 `ActivityResultContracts.GetMultipleContents()`를 활용하여 시스템 갤러리(선택 Intent)를 호출
  - 갤러리에서 다중 선택한 이미지 파일들은 `saveImageToInternalStorage` 메서드를 통해 앱의 내부 저장소(`filesDir`)로 복사 및 저장한 후, 영구적인 절대 파일 경로를 DB에 보관
  - 다중 이미지 중 사용자가 터치하여 대표 이미지(대표 사진)를 지정할 수 있는 기능을 제공하며, 상세 화면(`TripDetailFragment`)에서 다중 사진 썸네일 리사이클러뷰를 통해 갤러리처럼 조회 가능
---
### 3.6 기본 예외처리가 구현되어 있는가? ✅
- **구현 위치: `AddTripActivity.kt`, `TripDetailFragment.kt`
- **상세 내용:**
  - **입력 유효성 검증:** `AddTripActivity.kt`의 `validateForm()`에서 제목, 여행지, 시작일, 종료일이 비어있는지 검사하고 필수 입력을 누락한 경우 `TextInputLayout.error`를 지정하여 사용자에게 예외 상황을 시각적으로 알림
  - **파일 입출력 예외처리:** 갤러리 선택 이미지를 내부 저장소로 복사하는 `saveImageToInternalStorage` 처리 시 `try-catch` 블록으로 안전한 파일 복사 예외 처리 구현
  - **상세조회 예외처리:** `TripDetailFragment`에서 특정 여행기 데이터 조회 실패 시(`null`인 경우) 예외 처리를 통해 안내 메시지를 띄우고 이전 화면으로 안전하게 복귀하도록 제어
---
### 3.7 (선택)지도 API를 활용했는가? ✅
- **구현 위치:** `AddTripActivity.kt`, `res/layout/dialog_map_picker.xml`
- **상세 내용:**
  - Google Maps SDK를 도입하여 여행지 입력란(`etDestination`) 클릭 시 지도 다이얼로그(`dialog_map_picker`)가 표출
  - 다이얼로그에 탑재된 `MapView`를 통해 사용자는 마커를 드래그하거나 특정 지점을 탭하여 직접 위치를 지정 가능
  - 상단 `SearchView`를 통해 키워드로 목적지를 검색, Geocoder API를 연동하여 주소 검색 결과 위치(위도/경도)로 카메라를 이동시키고 마커 배치 가능
  - 최종 선택된 지점의 상세 주소 정보를 Geocoder의 역지오코딩을 통해 한글 텍스트 주소로 변환하여 자동으로 여행 목적지 입력창에 입력해줌
---
### 3.8 (선택)스레드 및 코루틴을 활용했는가? ✅
- **구현 위치:** `AddTripActivity.kt` (`getAddressFromLocation`, `getLocationFromAddressName`)
- **상세 내용:**
  - 지도 검색 및 마커 이동 시 Geocoder API를 이용한 위경도-주소 상호 변환 처리는 네트워크 조회가 발생하므로 메인(UI) 스레드를 차단하지 않고 `NetworkOnMainThreadException`을 예외를 방지하고자 코틀린의 백그라운드 스레드 `thread { ... }`를 별도로 생성하여 비동기 처리
  - 백그라운드 작업이 완료된 후 검색된 텍스트 주소를 텍스트뷰 및 입력 필드 등 UI 요소에 안전하게 갱신해주기 위해 `Handler(Looper.getMainLooper()).post { ... }`를 적용하여 UI 스레드로 복귀하도록 구현
---

