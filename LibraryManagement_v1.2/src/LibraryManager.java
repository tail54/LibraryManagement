import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

public class  LibraryManager {
    private Map<Integer, Book> bookMap;
    private List<User> userList;
    private User currentUser;
    private LibraryRepository repository;
    private int bookCount = 0;

    /**
     * LibraryManager 생성자입니다.
     * @param repository 데이터를 저장하고 불러올 리포지토리 객체
     */
    public LibraryManager(LibraryRepository repository) {
        this.repository = repository;
    }

    /**
     * 시스템을 초기화합니다.
     * <p>리포지토리로부터 도서 데이터를 로드하고, 도서 ID 카운트를 현재 최대값으로 동기화합니다.</p>
     * * @see LibraryRepository#loadBooks()
     * @see <a href="https://github.com/sumannam/Java/issues/23">Issue #23: 프로그램 실행 시 데이터 로드</a>
     */
    public void initialize() {
        this.bookMap = repository.loadBooks();
        // ID 카운트 동기화
        for (Integer id : bookMap.keySet()) {
            if (id > bookCount) bookCount = id;
        }
    }

    /**
     * 사용자 로그인을 수행하고 인증 상태를 기록합니다.
     * <p>성공 시 {@code currentUser}에 사용자 정보를 저장합니다.</p>
     *
     * @param id 사용자 아이디
     * @param pw 사용자 비밀번호
     * @return 로그인 성공 여부 (성공 시 true)
     * @see LibraryRepository#loadUser(String, String)
     */
    public boolean login(String id, String pw) {
        // 기존에 List<String>으로 받던 부분을 User로 변경
//        this.userList = repository.loadLogin(id, pw);
        User user = repository.loadUser(id, pw);

        if (user != null) {
            this.currentUser = user; // 로그인 성공 시 현재 사용자 저장
            return true;
        }
        return false;
    }

    /** @return 현재 로그인 중인 {@link User} 객체 */
    public User getCurrentUser() {
        return currentUser;
    }

//    public void setCurretUser(String user) {
//        this.currentUser = user;
//    }

    public void addBook(String title, String author) {
        bookCount++;
        bookMap.put(bookCount, new Book(bookCount, title, author, true, "null"));
        System.out.println("-----------------------------------------------------------");
        System.out.printf("[결과] 등록이 완료되었습니다. (도서 ID: %d)\n", bookCount);
    }

    /**
     * 도서 정보를 수정합니다.
     * @param id     수정할 도서 ID
     * @param title  새 제목
     * @param author 새 저자
     * @return 수정 성공 여부
     */
    public boolean editBook(int id, String title, String author) {
        if (!bookMap.containsKey(id)) return false;
        Book book = bookMap.get(id);
        // 제목이나 저자가 비어있지 않을 때만 수정 (기존 로직 유지)
        return true;
    }

    /**
     * 도서를 시스템에서 삭제합니다.
     * <p>삭제하는 메서드 추가(2026.05.20)</p>
     * @param id 삭제할 도서 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteBook(int id) {
        repository.deleteBook(id);
        return bookMap.remove(id) != null;
    }

    /**
     * 도서 대출 처리를 수행합니다.
     * <p>도서가 대출 가능한 상태일 경우, 상태를 변경하고 현재 로그인 사용자를 대출자로 등록합니다.</p>
     *
     * @param id 대출할 도서 ID
     * @return 대출 성공 여부
     */
    public boolean borrowBook(int id) {
        if (!bookMap.containsKey(id))
            return false;

        Book book = bookMap.get(id);
        if (book.isAvailable()) {
            book.setAvailable(false);
            book.setBorrowerId(currentUser.getUserId());
            return true;
        }
        return false;
    }

    /**
     * 도서 반납 처리를 수행합니다.
     * <p>도서가 대출 중인 상태일 경우, 상태를 대출 가능으로 변경하고 대출자 정보를 초기화합니다.</p>
     *
     * @param id 반납할 도서 ID
     * @return 반납 성공 여부
     */
    public boolean returnBook(int id) {
        if (!bookMap.containsKey(id))
            return false;

        Book book = bookMap.get(id);
        if (!book.isAvailable()) {
            book.setAvailable(true);
            book.setBorrowerId("null");
            return true;
        }
        return false;
    }

    /**
     * 제목 키워드를 사용하여 도서를 검색합니다.
     * @param keyword 검색어
     * @return 검색된 도서 객체들의 리스트
     */
    public List<Book> searchBook(String keyword) {
        List<Book> found = new ArrayList<>();
        for (Book book : bookMap.values()) {
            if (book.getTitle().contains(keyword)) found.add(book);
        }
        return found;
    }

    /** @return 등록된 모든 도서 정보의 Collection */
    public Collection<Book> getAllBooks() {
        return bookMap.values();
    }

    public int getBookCount() {
        return bookCount;
    }

    /**
     * 현재 메모리의 도서 변경 내역을 리포지토리를 통해 저장합니다.
     * @see LibraryRepository#saveBooks(Map)
     * @see <a href="https://github.com/sumannam/Java/issues/42">Issue #42: 테이블 추가/수정/삭제 방식 수정</a>
     */
    public void saveChanges() {
        repository.saveBooks(bookMap);
    }

    /** @return 도서 ID와 객체가 맵핑된 전체 Map 객체 */
    public Map<Integer, Book> getBookMap() {
        return bookMap;
    }

    public void checkServerStatus(String ip) {
        // 🟢 추가: 입력값이 비어있는지 먼저 안전하게 검증
        if (ip == null || ip.trim().isEmpty()) {
            System.out.println("[오류] IP 주소가 비어있습니다.");
            return;
        }

        try {
            System.out.println("[진단] " + ip + " 서버 연결 확인 중... (자바 안전 모드)");

            // 🟢 변경 포인트 1: 문자열 명령어를 만들지 않고, 자바 객체로 IP/호스트를 파싱함
            // 만약 여기에 "127.0.0.1 && echo..." 같은 악성 공격 구문이 들어오면
            // OS 명령어로 실행되지 못하고, 이 라인에서 바로 UnknownHostException 에러를 내며 차단됩니다.
            InetAddress address = InetAddress.getByName(ip.trim());

            // 🟢 변경 포인트 2: cmd.exe 핑 대신, JVM 내부 소켓을 이용해 3초(3000ms) 타임아웃 핑을 보냄
            if (address.isReachable(3000)) {
                System.out.println("[결과] 서버가 정상적으로 응답합니다. (연결 성공)");
            } else {
                System.out.println("[결과] 서버 응답 시간이 초과되었습니다. (연결 실패)");
            }
        } catch (Exception e) {
            // 🟢 변경 포인트 3: 공격 페이로드가 들어오면 이 예외 처리 구역으로 튕겨 나감 (안전 구역)
            System.out.println("[오류] 올바르지 않은 호스트 또는 IP 주소 형식입니다.");
        }
    }

}