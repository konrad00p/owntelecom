package pl.owntelecom.models;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Website {

    public enum SiteType {
        NORMAL,     // Zwykła strona/blog
        SHOP,       // Sklep online
        SOCIAL      // Social media
    }

    private final String id;
    private String name;
    private UUID owner;
    private String serverRoomId;
    private SiteType type;
    private String content; // Treść książki (BookMeta)
    private boolean active;
    private final long creationDate;
    private int visitCount;
    private final List<String> pages; // Strony książki
    private final Map<ItemStack, Double> shopItems; // Przedmioty w sklepie -> cena
    private String description;

    public Website(String id, String name, UUID owner, String serverRoomId, SiteType type) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.serverRoomId = serverRoomId;
        this.type = type;
        this.content = "";
        this.active = true;
        this.creationDate = System.currentTimeMillis();
        this.visitCount = 0;
        this.pages = new ArrayList<>();
        this.shopItems = new LinkedHashMap<>();
        this.description = "Nowa strona";
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public String getServerRoomId() { return serverRoomId; }
    public void setServerRoomId(String serverRoomId) { this.serverRoomId = serverRoomId; }
    public SiteType getType() { return type; }
    public void setType(SiteType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreationDate() { return creationDate; }
    public int getVisitCount() { return visitCount; }
    public List<String> getPages() { return pages; }
    public Map<ItemStack, Double> getShopItems() { return shopItems; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public void incrementVisits() {
        this.visitCount++;
    }

    public void addPage(String pageContent) {
        pages.add(pageContent);
    }

    public void addShopItem(ItemStack item, double price) {
        shopItems.put(item, price);
    }

    public void removeShopItem(int index) {
        if (index >= 0 && index < shopItems.size()) {
            List<ItemStack> keys = new ArrayList<>(shopItems.keySet());
            shopItems.remove(keys.get(index));
        }
    }

    public String getPreview() {
        if (pages.isEmpty()) return "Brak treści";
        String preview = pages.get(0);
        if (preview.length() > 50) preview = preview.substring(0, 47) + "...";
        return preview;
    }
}
