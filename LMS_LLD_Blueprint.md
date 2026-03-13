# Library Management System — LLD Blueprint
> **Purpose:** Single source of truth for design + implementation.
> Precise enough to regenerate identical code every time.
> Language: Pure Java (no frameworks, no advanced Java-specific concepts)
> Level: Staff Engineer Interview

---

## 1. REQUIREMENTS SNAPSHOT

| ID  | Rule |
|-----|------|
| R1  | Store books, members, and full transaction log (borrow, return, reserve, renew) |
| R2  | Every book has a unique ID and rack/location |
| R3  | Book metadata: ISBN, title, author, subject, publicationDate |
| R4  | One Book → Many BookItems (physical copies), each with unique ID |
| R5  | Two user types: Member and Librarian, both have LibraryCard with unique card number |
| R6  | Every user must have a LibraryCard |
| R7  | Member can borrow max 10 books at a time |
| R8  | Borrow period = 15 days |
| R9  | Only one member can reserve each BookItem at a time |
| R10 | Record who issued/reserved/renewed and on which date |
| R11 | Member can renew — max 2 renewals per borrow, resets 15-day window from renewal date |
| R12 | Notify member on overdue and when reserved book becomes available |
| R13 | If all BookItems of a Book are borrowed, member can reserve a specific BookItem |
| R14 | Single-field search: title, author, subject, or publicationDate |

---

## 2. BUSINESS RULES & CONSTANTS

```
MAX_BOOKS_PER_MEMBER        = 10
BORROW_PERIOD_DAYS          = 15
MAX_RENEWALS_PER_BORROW     = 2
RESERVATION_EXPIRY_DAYS     = 2
FINE_PER_DAY                = 2.0  (Rs)
FINE_PER_WEEK               = 10.0 (Rs)
Fine strategy                = Fixed per library (configured once at startup)
```

### Borrow Guard Conditions (Member)
A member CAN borrow if ALL of the following are true:
- Status is ACTIVE
- activeLendings.size() < 10
- outstandingFine == 0.0

### Reserve Guard Conditions (Member)
A member CAN reserve a BookItem if ALL of the following are true:
- Member does NOT already hold a copy of the same Book (by bookId)
- Member does NOT already have an active reservation for the same Book (by bookId)
- The BookItem is NOT already reserved by another member
- The BookItem status is BORROWED (i.e., not available on shelf)

### Renewal Guard Conditions
A lending CAN be renewed if:
- renewalCount < 2

### Reservation Expiry
- When a reserved BookItem is returned, reservation becomes AVAILABLE
- If member does not pick it up within 2 days → reservation expires
- On expiry: BookItem status → AVAILABLE, notify member of cancellation

---

## 3. ACTORS & ACTIVITIES

### Member Activities
1. Login / Logout
2. Register / Update account
3. Cancel membership
4. View account (details, history, checkouts, reservations, fines)
5. Search catalog (single field)
6. Reserve book (on BookItem)
7. Checkout / borrow book
8. Renew book
9. Return book
10. Remove reservation
11. Pay fine

### Librarian Activities
1. Login / Logout
2. Register / Update member account
3. Cancel membership
4. View member account
5. Issue library card
6. Add / Edit / Remove Book
7. Add / Edit / Remove BookItem
8. Issue book (on behalf of member)
9. Grant renewal
10. Update catalog
11. Remove reservation

### LMS (System Actor) Activities
1. Calculate fine
2. Send overdue notification
3. Send reservation available notification
4. Send reservation cancelled notification

---

## 4. ENTITY MODEL

### 4.1 Enums

```
BookItemStatus  : AVAILABLE, BORROWED, RESERVED, LOST
MemberStatus    : ACTIVE, CANCELLED, BLACKLISTED
TransactionType : BORROWED, RETURNED, RENEWED, RESERVED,
                  RESERVATION_CANCELLED, FINE_PAID
FineStrategyType: PER_DAY, PER_WEEK
```

### 4.2 Core Entities

#### LibraryCard
```
- cardNumber   : String   [unique]
- issuedDate   : Date
- isActive     : boolean
```

#### User (abstract)
```
- name         : String
- email        : String
- phone        : String
- password     : String
- libraryCard  : LibraryCard
+ getRole()    : String   [abstract]
```

#### Member extends User
```
- status              : MemberStatus
- activeLendings      : List<BookLending>
- activeReservations  : List<Reservation>
- borrowingHistory    : List<BookLending>
- outstandingFine     : double

+ canBorrow()                        : boolean
+ alreadyHasCopyOfBook(bookId)       : boolean
+ alreadyReservedBook(bookId)        : boolean
+ addLending(BookLending)            : void
+ removeLending(BookLending)         : void
+ addReservation(Reservation)        : void
+ removeReservation(Reservation)     : void
+ addFine(amount)                    : void
+ payFine(amount)                    : void
+ getRole()                          : "MEMBER"
```

#### Librarian extends User
```
+ getRole() : "LIBRARIAN"
```

#### Book
```
- bookId          : String   [unique]
- isbn            : String
- title           : String
- author          : String
- subject         : String
- publicationDate : String
- bookItems       : List<BookItem>

+ addBookItem(BookItem)    : void
+ removeBookItem(String)   : void
+ getAvailableItems()      : List<BookItem>
```

#### BookItem
```
- bookItemId   : String   [unique]
- bookId       : String   [FK → Book]
- rackLocation : String
- status       : BookItemStatus
- reservation  : Reservation  [nullable — at most one]

+ isAvailable() : boolean
```

#### BookLending  [Transaction Record]
```
- lendingId      : String   [unique]
- bookItem       : BookItem
- member         : Member
- issuedBy       : String   [librarian card number OR member card number]
- borrowDate     : Date
- dueDate        : Date     [borrowDate + 15 days]
- returnDate     : Date     [nullable until returned]
- renewalCount   : int      [0..2]
- isClosed       : boolean

+ renew()        : boolean  [returns false if renewalCount >= 2]
+ isOverdue()    : boolean
+ getDaysOverdue(): int
```

#### Reservation  [Transaction Record]
```
- reservationId       : String   [unique]
- bookItem            : BookItem
- member              : Member
- reservedDate        : Date
- availableDate       : Date     [nullable — set when BookItem is returned]
- expiryDate          : Date     [nullable — set when availableDate is set; availableDate + 2 days]
- isCancelled         : boolean

+ isExpired()         : boolean  [checks expiryDate vs today]
```

#### TransactionLog  [Audit]
```
- logId           : String
- memberCardNumber : String
- bookItemId      : String
- transactionType : TransactionType
- transactionDate : Date
- remarks         : String
```

---

## 5. INTERFACES (Contracts)

### FineCalculator
```
+ calculate(overdueDays: int) : double
```
Implementations:
- PerDayFineCalculator  → overdueDays * 2.0
- PerWeekFineCalculator → Math.ceil(overdueDays / 7.0) * 10.0

### NotificationService
```
+ sendOverdueNotification(memberEmail, memberPhone, bookTitle)              : void
+ sendReservationAvailableNotification(memberEmail, memberPhone, bookTitle) : void
+ sendReservationCancelledNotification(memberEmail, memberPhone, bookTitle) : void
```
Implementations:
- EmailNotificationService  → dummy System.out simulating email send
- SmsNotificationService    → dummy System.out simulating SMS send
- CompositeNotificationService → holds List<NotificationService>, delegates to all

### SearchService
```
+ searchByTitle(title: String)                   : List<Book>
+ searchByAuthor(author: String)                 : List<Book>
+ searchBySubject(subject: String)               : List<Book>
+ searchByPublicationDate(publicationDate: String): List<Book>
```
Implementation: InMemorySearchService (iterates BookRepository)

### BookRepository
```
+ addBook(Book)                              : void
+ removeBook(bookId: String)                 : void
+ findBookById(bookId: String)               : Book
+ getAllBooks()                              : List<Book>
+ addBookItem(BookItem)                      : void
+ removeBookItem(bookItemId: String)         : void
+ findBookItemById(bookItemId: String)       : BookItem
+ findBookItemsByBookId(bookId: String)      : List<BookItem>
```
Implementation: InMemoryBookRepository (uses HashMap internally)

### MemberRepository
```
+ addMember(Member)                          : void
+ removeMember(cardNumber: String)           : void
+ findByCardNumber(cardNumber: String)       : Member
+ getAllMembers()                            : List<Member>
```
Implementation: InMemoryMemberRepository (uses HashMap internally)

---

## 6. DESIGN PATTERNS APPLIED

| Pattern   | Applied To                  | Why |
|-----------|-----------------------------|-----|
| Strategy  | FineCalculator              | PerDay vs PerWeek swappable at runtime without changing business logic |
| Composite | NotificationService         | Send via Email + SMS simultaneously using same interface |
| Facade    | Library class               | Single entry point; hides all subsystem complexity from caller |
| Repository| BookRepository, MemberRepository | Decouples storage mechanism from business logic |
| Observer  | NotificationService (manual)| LMS notifies members on key events (overdue, reservation) |

---

## 7. SERVICE LAYER

### BookService
Responsibilities: manage Book and BookItem CRUD
```
- bookRepository : BookRepository

+ addBook(bookId, isbn, title, author, subject, pubDate) : Book
+ editBook(bookId, title, author, subject, pubDate)      : void
+ removeBook(bookId)                                     : void
+ addBookItem(bookId, bookItemId, rackLocation)          : BookItem
+ editBookItem(bookItemId, rackLocation)                 : void
+ removeBookItem(bookItemId)                             : void
+ getBook(bookId)                                        : Book
+ getBookItem(bookItemId)                                : BookItem
+ findBookItemsByBookId(bookId)                          : List<BookItem>
```

### MemberService
Responsibilities: manage Member lifecycle
```
- memberRepository : MemberRepository

+ registerMember(name, email, phone, password, cardNumber) : Member
+ updateMember(cardNumber, name, email, phone)             : void
+ cancelMembership(cardNumber)                             : void
+ getMember(cardNumber)                                    : Member
+ issueLibraryCard(cardNumber)                             : LibraryCard
```

### LendingService
Responsibilities: all borrow/return/renew operations
```
- bookRepository    : BookRepository
- memberRepository  : MemberRepository
- fineCalculator    : FineCalculator
- notificationService: NotificationService
- transactionLog    : List<TransactionLog>

+ borrowBook(memberCardNumber, bookItemId, issuedByCardNumber)  : BookLending
+ returnBook(memberCardNumber, bookItemId)                      : double  [returns fine amount]
+ renewBook(memberCardNumber, bookItemId)                       : BookLending
+ checkAndNotifyOverdue()                                       : void   [LMS scheduled call]
```

### ReservationService
Responsibilities: all reservation operations
```
- bookRepository    : BookRepository
- memberRepository  : MemberRepository
- notificationService: NotificationService
- transactionLog    : List<TransactionLog>

+ reserveBookItem(memberCardNumber, bookItemId)  : Reservation
+ cancelReservation(memberCardNumber, bookItemId): void
+ checkAndExpireReservations()                   : void  [LMS scheduled call]
+ notifyReservationAvailable(bookItemId)         : void  [called after book return]
```

### SearchService (Implementation: InMemorySearchService)
```
- bookRepository : BookRepository

+ searchByTitle(title)               : List<Book>
+ searchByAuthor(author)             : List<Book>
+ searchBySubject(subject)           : List<Book>
+ searchByPublicationDate(pubDate)   : List<Book>
```

---

## 8. FACADE — Library Class

Single entry point. Wires all services together.
```
- bookService        : BookService
- memberService      : MemberService
- lendingService     : LendingService
- reservationService : ReservationService
- searchService      : SearchService

// Member operations
+ searchBooks(field, value)                                  : List<Book>
+ borrowBook(memberCardNumber, bookItemId)                   : BookLending
+ returnBook(memberCardNumber, bookItemId)                   : double
+ renewBook(memberCardNumber, bookItemId)                    : BookLending
+ reserveBookItem(memberCardNumber, bookItemId)              : Reservation
+ cancelReservation(memberCardNumber, bookItemId)            : void
+ payFine(memberCardNumber, amount)                          : void
+ viewAccount(memberCardNumber)                              : Member

// Librarian operations
+ addBook(...)                                               : Book
+ editBook(...)                                              : void
+ removeBook(bookId)                                         : void
+ addBookItem(...)                                           : BookItem
+ editBookItem(...)                                          : void
+ removeBookItem(bookItemId)                                 : void
+ registerMember(...)                                        : Member
+ updateMember(...)                                          : void
+ cancelMembership(cardNumber)                               : void
+ issueBookOnBehalf(libCardNumber, memberCardNumber, bookItemId): BookLending
+ grantRenewal(libCardNumber, memberCardNumber, bookItemId)  : BookLending
+ removeReservation(libCardNumber, memberCardNumber, bookItemId): void

// LMS system operations
+ runOverdueCheck()                                          : void
+ runReservationExpiryCheck()                               : void
```

---

## 9. FILE STRUCTURE

```
src/
├── constants/
│   └── LibraryConstants.java
├── enums/
│   ├── BookItemStatus.java
│   ├── MemberStatus.java
│   ├── TransactionType.java
│   └── FineStrategyType.java
├── models/
│   ├── LibraryCard.java
│   ├── User.java               [abstract]
│   ├── Member.java
│   ├── Librarian.java
│   ├── Book.java
│   ├── BookItem.java
│   ├── BookLending.java
│   ├── Reservation.java
│   └── TransactionLog.java
├── interfaces/
│   ├── FineCalculator.java
│   ├── NotificationService.java
│   ├── SearchService.java
│   ├── BookRepository.java
│   └── MemberRepository.java
├── fine/
│   ├── PerDayFineCalculator.java
│   └── PerWeekFineCalculator.java
├── notification/
│   ├── EmailNotificationService.java
│   ├── SmsNotificationService.java
│   └── CompositeNotificationService.java
├── repository/
│   ├── InMemoryBookRepository.java
│   └── InMemoryMemberRepository.java
├── services/
│   ├── BookService.java
│   ├── MemberService.java
│   ├── LendingService.java
│   ├── ReservationService.java
│   └── InMemorySearchService.java
├── facade/
│   └── Library.java
└── Main.java
```

---

## 10. KEY FLOWS (Sequence Logic)

### Borrow Flow
```
1. Fetch Member by cardNumber → validate ACTIVE
2. Check canBorrow() → size < 10 AND fine == 0
3. Fetch BookItem by bookItemId → validate AVAILABLE
4. If BookItem has reservation → check if reservation belongs to THIS member
   - If reserved by another member → REJECT
5. Set BookItem.status = BORROWED
6. Create BookLending (borrowDate=today, dueDate=today+15)
7. Add lending to member.activeLendings
8. Clear reservation if it existed for this member
9. Log transaction: BORROWED
```

### Return Flow
```
1. Fetch Member → Fetch BookItem
2. Find active BookLending for this member + bookItem
3. Calculate fine if overdue → addFine to member
4. Set lending.returnDate = today, lending.isClosed = true
5. Remove from member.activeLendings
6. Check if BookItem has a pending reservation
   - If YES → set BookItem.status = RESERVED
             set reservation.availableDate = today
             set reservation.expiryDate = today + 2
             notify reserved member: reservation available
   - If NO  → set BookItem.status = AVAILABLE
7. Log transaction: RETURNED
8. If fine > 0: notify member of overdue fine
```

### Reserve Flow
```
1. Fetch Member → validate ACTIVE
2. Fetch BookItem → validate status is BORROWED (not AVAILABLE)
3. Check member does not already hold copy of same book
4. Check member does not already have reservation for same book
5. Check BookItem.reservation == null (no existing reservation)
6. Create Reservation (reservedDate=today)
7. Set BookItem.reservation = new Reservation
8. Add reservation to member.activeReservations
9. Log transaction: RESERVED
```

### Renew Flow
```
1. Fetch Member → Fetch BookItem
2. Find active BookLending for this member + bookItem
3. Check lending.renewalCount < 2
4. Increment lending.renewalCount
5. Set lending.dueDate = today + 15
6. Log transaction: RENEWED
```

### Reservation Expiry Check (LMS scheduled)
```
For each BookItem with status = RESERVED:
  Find its Reservation
  If reservation.expiryDate < today AND NOT cancelled:
    Set reservation.isCancelled = true
    Set BookItem.status = AVAILABLE
    Remove reservation from member.activeReservations
    Log transaction: RESERVATION_CANCELLED
    Notify member: reservation cancelled
```

### Overdue Check (LMS scheduled)
```
For each Member:
  For each active BookLending:
    If lending.isOverdue():
      Notify member: book overdue
```

---

## 11. EXCEPTION / VALIDATION RULES

| Scenario                                      | Action |
|-----------------------------------------------|--------|
| Member borrows > 10 books                     | Throw / return error message |
| Member has outstanding fine tries to borrow   | Throw / return error message |
| BookItem is BORROWED and member tries to borrow| Throw / return error message |
| BookItem reserved by another, member tries to borrow | Throw / return error message |
| Member tries to reserve their own borrowed copy | Throw / return error message |
| Member tries to reserve already-reserved item | Throw / return error message |
| Renewal count >= 2                            | Throw / return error message |
| Inactive member tries any action              | Throw / return error message |

---

## 12. MAIN — Demo Scenario (End-to-End)

```
1. Setup fine strategy: PerDayFineCalculator
2. Setup notification: CompositeNotificationService (Email + SMS)
3. Create Library facade
4. Librarian registers member "Anki"
5. Librarian adds Book "Clean Code" with 2 BookItems
6. Member Anki searches by author "Robert Martin"
7. Member Anki borrows BookItem-1
8. Member Anki renews BookItem-1
9. Second member "Bob" tries to reserve BookItem-1 (succeeds — it's borrowed)
10. Member Anki returns BookItem-1 → triggers reservation-available notification to Bob
11. Bob borrows BookItem-1 (fulfills reservation)
12. LMS runs overdue check → no overdues
13. LMS runs reservation expiry check
14. Member Anki pays fine (if any)
```

---

## 13. DESIGN DECISIONS & REASONING

| Decision | Reasoning |
|----------|-----------|
| Reservation is on BookItem, not Book | Requirement explicitly states physical copy reservation |
| FineCalculator as Strategy | Fine policy can change without touching LendingService |
| CompositeNotificationService | Open/Closed — add new channel without changing existing code |
| Repository interfaces | Swap in-memory for DB later without touching services |
| Library as Facade | Interviewer sees clean API; internal complexity hidden |
| No frameworks | Portability to C++/Python/C#; shows pure OOP thinking |
| Abstract User, not interface | User has shared state (name, email, card); interface would force duplication |
| TransactionLog as separate entity | R1/R10 explicitly require audit trail; not mixed into models |
| BookLending holds renewalCount | Renewal is a property of a lending transaction, not of a book |
| Reservation holds availableDate + expiryDate | Expiry logic needs both dates; computed at return time |
