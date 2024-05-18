package deep.clone;

import java.util.List;

class Man {
    private String name;
    private int age;
    private List<String> favoriteBooks;

    public Man(String name, int age, List<String> favoriteBooks) {
        this.name = name;
        this.age = age;
        this.favoriteBooks = favoriteBooks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<String> getFavoriteBooks() {
        return favoriteBooks;
    }

    public void setFavoriteBooks(List<String> favoriteBooks) {
        this.favoriteBooks = favoriteBooks;
    }

    public static void main(String[] args) {
        Man man = new Man(
                "Alex",
                25,
                List.of("Book 1")
        );
        try {
            Man clone = CopyUtils.deepClone(man);

            System.out.println(man);
            System.out.println(clone);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}