@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookListVO {
    private Integer id;
    private String title;
    private Integer creatorId;
    private String description;
    private String picture;
    private List<ProductVO> products;

    private String creatorName;
    private String creatorAvatar;

    private LocalDateTime creationDate;
    private Integer favouriteCount;
} 