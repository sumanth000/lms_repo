package com.libraryzone.resource;

import java.util.*;

import com.libraryzone.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class apiFile {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/check")
    public String getString() {
        String responseString = "running properly";
        return responseString;
    }


    @GetMapping("/search/basis/book")
    public List<BookSearchResponse> searchBooks(
            @RequestParam(name = "isbn", required = false) String isbn,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "year", required = false) Integer year) {

        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (isbn != null) {
            sql.append(" AND isbn = ?");
            params.add(isbn);
        }
        if (title != null) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title + "%");
        }
        if (author != null) {
            sql.append(" AND author LIKE ?");
            params.add("%" + author + "%");
        }
        if (genre != null) {
            sql.append(" AND genre LIKE ?");
            params.add("%" + genre + "%");
        }
        if (year != null) {
            sql.append(" AND publication_year = ?");
            params.add(year);
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), new BeanPropertyRowMapper<>(BookSearchResponse.class));
    }

    @GetMapping("/searching/basis/copies")
    public List<BookSearchResponse> searchBookBasisCopies(String isbn, String title, String author, String genre, Integer year) {
        StringBuilder sql = new StringBuilder("SELECT b.*, bc.serial_number, bc.availability_status FROM books b " +
                "LEFT JOIN book_copies bc ON b.isbn = bc.isbn " +
                "WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (isbn != null) {
            sql.append(" AND b.isbn = ?");
            params.add(isbn);
        }
        if (title != null) {
            sql.append(" AND b.title LIKE ?");
            params.add("%" + title + "%");
        }
        if (author != null) {
            sql.append(" AND b.author LIKE ?");
            params.add("%" + author + "%");
        }
        if (genre != null) {
            sql.append(" AND b.genre LIKE ?");
            params.add("%" + genre + "%");
        }
        if (year != null) {
            sql.append(" AND b.publication_year = ?");
            params.add(year);
        }

        Map<String, BookSearchResponse> bookMap = new LinkedHashMap<>(); // Map to store unique books by ISBN

        jdbcTemplate.query(sql.toString(), params.toArray(), rs -> {
            String currentIsbn = rs.getString("isbn");
            BookSearchResponse book = bookMap.get(currentIsbn);
            if (book == null) {
                book = new BookSearchResponse();
                book.setIsbn(currentIsbn);
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setGenre(rs.getString("genre"));
                book.setPublicationYear(rs.getInt("publication_year"));
                book.setCopies(new ArrayList<>());
                bookMap.put(currentIsbn, book);
            }

            // Populate book copy details
            if (rs.getObject("serial_number") != null) {
                BookCopy copy = new BookCopy();
                copy.setSerialNumber(rs.getInt("serial_number"));
                copy.setAvailabilityStatus(rs.getBoolean("availability_status"));
                book.getCopies().add(copy);
            }

        });

        return new ArrayList<>(bookMap.values());
    }

    @PostMapping("/add/book/copies")
    public ResponseEntity<String> addBookAndCopies(@RequestBody BookVo bookVO) {
        try {
            // Add the book details to the 'books' table
            String insertBookSql = "INSERT INTO books (isbn, title, author, genre, publication_year) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertBookSql, bookVO.getIsbn(), bookVO.getTitle(), bookVO.getAuthor(), bookVO.getGenre(), bookVO.getPublicationYear());

            // If copies are specified, insert records into 'book_copies' table
            int copies = bookVO.getCopies(); // Get the number of copies from the request
            if (copies > 0) {
                insertBookCopies(bookVO.getIsbn(), copies); // Insert book copies into 'book_copies' table
            }

            return ResponseEntity.ok("Book added successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add book");
        }
    }

    private void insertBookCopies(String isbn, int copies) {
        // Implement logic to insert book copies into 'book_copies' table
        // This is a placeholder method; replace it with actual database interaction logic
        // Example: Execute SQL INSERT statements
        String insertCopySql = "INSERT INTO book_copies (isbn,availability_status) VALUES (?,TRUE)";
        for (int i = 0; i < copies; i++) {
            jdbcTemplate.update(insertCopySql, isbn);
        }
    }

    @PostMapping("/add/book")
    public ResponseEntity<String> addBook(@RequestBody BookVo bookVO) {
        String sql = "INSERT INTO books (isbn, title, author, genre, publication_year) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookVO.getIsbn(), bookVO.getTitle(), bookVO.getAuthor(), bookVO.getGenre(), bookVO.getPublicationYear());

        // Add logic to add multiple copies of the book with different serial numbers if needed

        return ResponseEntity.ok("Book added successfully!");
    }

    @PostMapping("/add/borrower")
    public ResponseEntity<String> addBorrower(@RequestBody BorrowerVo borrowerVO) {
        String sql = "INSERT INTO borrowers (name, email, contact_number) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, borrowerVO.getName(), borrowerVO.getEmail(), borrowerVO.getContactNumber());

        return ResponseEntity.ok("Borrower added successfully!");
    }


    @PostMapping("/borrow")
    public ResponseEntity<String> borrowBook(@RequestParam int serialNumber, @RequestParam String isbn, @RequestParam int borrowerId) {
        // Check if the book copy is available
        String checkAvailabilitySql = "SELECT availability_status FROM book_copies WHERE serial_number = ? AND isbn = ?";
        boolean isAvailable = jdbcTemplate.queryForObject(checkAvailabilitySql, Boolean.class, serialNumber, isbn);
        if (!isAvailable) {
            return ResponseEntity.badRequest().body("The book copy is not available for borrowing.");
        }

        // Insert borrowing record
        String borrowSql = "INSERT INTO borrowing_history (serial_number, book_isbn, borrower_id, borrowing_date) VALUES (?, ?, ?, CURRENT_DATE)";
        jdbcTemplate.update(borrowSql, serialNumber, isbn, borrowerId);

        // Update book copy availability status
        String updateAvailabilitySql = "UPDATE book_copies SET availability_status = FALSE WHERE serial_number = ? AND isbn = ?";
        jdbcTemplate.update(updateAvailabilitySql, serialNumber, isbn);

        return ResponseEntity.ok("Book borrowed successfully!");
    }


    @PostMapping("/return")
    public ResponseEntity<String> returnBook(@RequestParam int transactionId, @RequestParam int serialNumber, @RequestParam String isbn) {
        // Check if transaction exists
        String checkTransactionSql = "SELECT COUNT(*) FROM borrowing_history WHERE transaction_id = ?";
        int transactionCount = jdbcTemplate.queryForObject(checkTransactionSql, Integer.class, transactionId);
        if (transactionCount == 0) {
            return ResponseEntity.badRequest().body("Invalid transaction ID.");
        }

        // Update borrowing record with return date
        String returnSql = "UPDATE borrowing_history SET return_date = CURRENT_DATE WHERE transaction_id = ?";
        jdbcTemplate.update(returnSql, transactionId);

        // Update book copy availability status
        String updateAvailabilitySql = "UPDATE book_copies SET availability_status = TRUE WHERE serial_number = ? AND isbn = ?";
        jdbcTemplate.update(updateAvailabilitySql, serialNumber, isbn);

        return ResponseEntity.ok("Book returned successfully!");
    }


//    @GetMapping("/borrowing-history")
//    public List<BorrowingHistoryVo> getBorrowingHistory(@RequestParam int borrowerId) {
//        try {
//            String sql = "SELECT bh.*, bc.availability_status FROM borrowing_history bh " +
//                    "INNER JOIN book_copies bc ON bh.serial_number = bc.serial_number AND bh.book_isbn = bc.isbn " +
//                    "WHERE bh.borrower_id = ? OR bh.return_date IS NULL";
//            return jdbcTemplate.query(sql, new Object[]{borrowerId}, (rs, rowNum) -> {
//                BorrowingHistoryVo history = new BorrowingHistoryVo();
//                history.setTransactionId(rs.getInt("transaction_id"));
//                history.setSerialNumber(rs.getInt("serial_number"));
//                history.setIsbn(rs.getString("isbn"));
//                history.setBorrowerId(rs.getInt("borrower_id"));
//                history.setBorrowingDate(rs.getDate("borrowing_date").toLocalDate());
//                history.setReturnDate(rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null);
//                history.setAvailabilityStatus(rs.getBoolean("availability_status"));
//                return history;
//            });
//        } catch (DataAccessException e) {
//            // Log the exception or handle it appropriately
//            e.printStackTrace();
//            return Collections.emptyList(); // Return empty list in case of exception
//        }
//    }
    @GetMapping("/borrowing-history")
    public ResponseEntity<List<Map<String, Object>>> getBorrowingHistory(@RequestParam int borrowerId) {
        try {
            String sql = "SELECT bh.transaction_id, bh.borrower_id, bh.book_isbn, bh.borrowing_date, bh.return_date, " +
                    "bc.serial_number, bc.availability_status " +
                    "FROM borrowing_history bh " +
                    "INNER JOIN book_copies bc ON bh.serial_number = bc.serial_number AND bh.book_isbn = bc.isbn " +
                    "WHERE bh.borrower_id = ? ";

            List<Map<String, Object>> borrowingHistoryList = jdbcTemplate.queryForList(sql, borrowerId);

            return ResponseEntity.ok(borrowingHistoryList);
        } catch (DataAccessException e) {
            // Log the exception or handle it appropriately
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Return internal server error in case of exception
        }
    }




}
