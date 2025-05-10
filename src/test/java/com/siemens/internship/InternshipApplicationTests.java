package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnableAsync
class InternshipApplicationTests {
	private MockMvc mockMvc;

	@Mock
	private ItemService itemService;

	@InjectMocks
	private ItemController itemController;

	@Mock
	private ItemRepository itemRepository;

	private ObjectMapper objectMapper;
	private Item item;
	private List<Item> processedItems;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(itemController)
				.build();
		objectMapper = new ObjectMapper();
		item = new Item(1L, "Item 1", "Description", "UNPROCESSED", "albumaria23@gmail.com");

		processedItems = Arrays.asList(
				new Item(1L, "Item 1", "Description 1", "PROCESSED", "test1@example.com"),
				new Item(2L, "Item 2", "Description 2", "PROCESSED", "test2@example.com"),
				new Item(3L, "Item 3", "Description 3", "PROCESSED", "test3@example.com")
		);
	}


	@Test
	void testGetAllItems_Controller() throws Exception {
		List<Item> items = Collections.singletonList(item);
		when(itemService.findAll()).thenReturn(items);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Item 1"))
				.andExpect(jsonPath("$[0].description").value("Description"))
				.andExpect(jsonPath("$[0].status").value("UNPROCESSED"))
				.andExpect(jsonPath("$[0].email").value("albumaria23@gmail.com"));

		verify(itemService, times(1)).findAll();
	}

	@Test
	void testCreateItemSuccess_Controller() throws Exception {
		Item newItem = new Item(5L, "New Item", "New Description", "UNPROCESSED", "email@email.com");

		when(itemService.save(Mockito.any(Item.class))).thenReturn(newItem);
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newItem)))
				.andExpect(status().isCreated());
	}

	@Test
	void testCreateItemValidationError_Controller() throws Exception {
		Item invalidItem = new Item(5L, "New Item", "New Description", "UNPROCESSED", "email.wrongEmail.com");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidItem)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testGetItemById_Controller() throws Exception {
		when(itemService.findById(item.getId())).thenReturn(Optional.of(item));

		mockMvc.perform(get("/api/items/{id}", item.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Item 1"))
				.andExpect(jsonPath("$.description").value("Description"))
				.andExpect(jsonPath("$.status").value("UNPROCESSED"))
				.andExpect(jsonPath("$.email").value("albumaria23@gmail.com"));
	}

	@Test
	void testUpdateItemSuccess_Controller() throws Exception {
		Item updatedItem = new Item(1L, "Updated Name", "Updated Description", "PROCESSED", "updated@email.com");

		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		when(itemService.save(Mockito.any(Item.class))).thenReturn(updatedItem);

		mockMvc.perform(put("/api/items/{id}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Name"))
				.andExpect(jsonPath("$.description").value("Updated Description"))
				.andExpect(jsonPath("$.status").value("PROCESSED"))
				.andExpect(jsonPath("$.email").value("updated@email.com"));
	}

	@Test
	void testUpdateItemNotFound_Controller() throws Exception {
		Item updatedItem = new Item(99L, "Name", "Description", "PROCESSED", "updated@email.com");

		when(itemService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/items/{id}", 99L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isNotFound());
	}

	@Test
	void testDeleteItemSuccess_Controller() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/{id}", 1L))
				.andExpect(status().isOk());

		verify(itemService, times(1)).deleteById(1L);
	}

	@Test
	void testDeleteItemNotFound_Controller() throws Exception {
		when(itemService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/items/{id}", 99L))
				.andExpect(status().isNotFound());

		verify(itemService, never()).deleteById(anyLong());
	}

	@Test
	void testProcessAllItemsSuccess_Controller() throws Exception {
		when(itemService.processAllItemsAsync()).thenReturn(CompletableFuture.completedFuture(processedItems));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].status").value("PROCESSED"))
				.andExpect(jsonPath("$[1].status").value("PROCESSED"))
				.andExpect(jsonPath("$[2].status").value("PROCESSED"));

		verify(itemService, times(1)).processAllItemsAsync();
	}

	@Test
	void testProcessAllItemsDirect_Controller() {
		when(itemService.processAllItemsAsync()).thenReturn(CompletableFuture.completedFuture(processedItems));

		ResponseEntity<List<Item>> response = itemController.processAllItems();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("PROCESSED", response.getBody().get(0).getStatus());
		assertEquals("PROCESSED", response.getBody().get(1).getStatus());
		assertEquals("PROCESSED", response.getBody().get(2).getStatus());

		verify(itemService, times(1)).processAllItemsAsync();
	}

	@Test
	void testProcessAllItemsDirect_Controller_WithException() {
		CompletableFuture<List<Item>> exceptionalFuture = new CompletableFuture<>();
		exceptionalFuture.completeExceptionally(new RuntimeException("Processing failed"));
		when(itemService.processAllItemsAsync()).thenReturn(exceptionalFuture);

		ResponseEntity<List<Item>> response = itemController.processAllItems();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		verify(itemService, times(1)).processAllItemsAsync();
	}


	@Test
	void testProcessAllItemsAsync_Service() throws Exception {
		List<Long> itemIds = Arrays.asList(1L, 2L, 3L);

		when(itemRepository.findAllIds()).thenReturn(itemIds);
        for (Long id : itemIds) {
            Item item = new Item(id, "Item " + id, "Description " + id, "UNPROCESSED", "test" + id + "@example.com");
            Item processedItem = new Item(id, "Item " + id, "Description " + id, "PROCESSED", "test" + id + "@example.com");

            when(itemRepository.findById(id)).thenReturn(Optional.of(item));
            when(itemRepository.save(any(Item.class))).thenReturn(processedItem);
        }

		ItemService realItemService = new ItemService(itemRepository);

		CompletableFuture<List<Item>> future = realItemService.processAllItemsAsync();
		List<Item> result = future.get();  // Wait for completion

		assertThat(result).hasSize(3);
		for (Item item : result) {
			assertThat(item.getStatus()).isEqualTo("PROCESSED");
		}

		verify(itemRepository, times(1)).findAllIds();
		verify(itemRepository, times(3)).findById(anyLong());
		verify(itemRepository, times(3)).save(any(Item.class));
	}

	@Test
	void testProcessAllItemsAsync_Service_WithNullItem() throws Exception {
		List<Long> itemIds = Arrays.asList(1L, 2L, 3L);

		when(itemRepository.findAllIds()).thenReturn(itemIds);

		Item item1 = new Item(1L, "Item 1", "Description 1", "UNPROCESSED", "test1@example.com");
		Item processedItem1 = new Item(1L, "Item 1", "Description 1", "PROCESSED", "test1@example.com");
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
		when(itemRepository.save(item1)).thenReturn(processedItem1);

		when(itemRepository.findById(2L)).thenReturn(Optional.empty());

		Item item3 = new Item(3L, "Item 3", "Description 3", "UNPROCESSED", "test3@example.com");
		Item processedItem3 = new Item(3L, "Item 3", "Description 3", "PROCESSED", "test3@example.com");
		when(itemRepository.findById(3L)).thenReturn(Optional.of(item3));
		when(itemRepository.save(item3)).thenReturn(processedItem3);

		ItemService realItemService = new ItemService(itemRepository);

		CompletableFuture<List<Item>> future = realItemService.processAllItemsAsync();
		List<Item> result = future.get();

		assertThat(result).hasSize(2);
		assertThat(result.stream().map(Item::getId)).containsExactlyInAnyOrder(1L, 3L);

		verify(itemRepository, times(1)).findAllIds();
		verify(itemRepository, times(3)).findById(anyLong());
		verify(itemRepository, times(2)).save(any(Item.class));
	}

	@Test
	void testProcessAllItemsAsync_ServiceWithInterruptedException() {
		List<Long> itemIds = Arrays.asList(1L, 2L);

		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenAnswer(invocation -> {
			Thread.sleep(50);
			return Optional.of(new Item(1L, "Item 1", "Description 1", "UNPROCESSED", "test1@example.com"));
		});

		when(itemRepository.findById(2L)).thenAnswer(invocation -> {
			Thread.currentThread().interrupt();
			throw new InterruptedException("Test interruption");
		});

		ItemService realItemService = new ItemService(itemRepository);

		CompletableFuture<List<Item>> future = realItemService.processAllItemsAsync();

		ExecutionException exception = assertThrows(ExecutionException.class, future::get);
		assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
		assertThat(exception.getCause().getMessage()).contains("Error processing item with ID: 2");
	}
}
