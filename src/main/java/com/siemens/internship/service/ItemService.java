package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private static ExecutorService itemProcessingExecutor;

    // These 2 fields are not used / read from anymore, so I have removed them, with some notes of how I would have
    // handled them if it still were necessary for me to use them

    // Changed the processed items list into a synchronized list collection such that when adding there are no concurrency issues
    // It is important to use synchronized collections when working with asynchronous functions
    // private final List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

    // Similar to processedItems, using AtomicInteger instead of a regular integer ensures thread-safe increments,
    // avoiding concurrency issues since AtomicInteger handles synchronization internally
    // private final AtomicInteger processedCount = new AtomicInteger(0);

    @Autowired
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        itemProcessingExecutor = Executors.newFixedThreadPool(10);
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    public Item save(Item newItem) {
        return itemRepository.save(newItem);
    }

    public void deleteById(Long itemId) {
        itemRepository.deleteById(itemId);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation

     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    // the @Async annotation means that this method will run on a separate thread, without blocking the main execution
    // thread. That's why this method can run in the background and eventually complete the future that it is returning
    @Async
    public CompletableFuture<List<Item>> processAllItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Item>> obtainedFuturesList = new ArrayList<>();

        for (Long itemId : itemIds) {
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);

                    Item item = itemRepository.findById(itemId).orElse(null);
                    if (item == null) {
                        return null;
                    }

                    item.setStatus("PROCESSED");
                    return itemRepository.save(item);

                } catch (InterruptedException interruptedException) {
                    System.out.println("Error: " + interruptedException.getMessage());
                    throw new RuntimeException("Error processing item with ID: " + itemId, interruptedException);
                }
            }, itemProcessingExecutor);
            obtainedFuturesList.add(future);
        }

        // previously, the CompletableFutures ran independently of each other without any result collection
        // now, in order for all of them to complete before continuing, we coordinate them using the call of CompletableFuture.allOf()

        // I've used CompletableFuture<Void> here since we only care about when all tasks finish, not their individual return types
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(obtainedFuturesList.toArray(new CompletableFuture[0]));

        // when all the futures are done, we should collect the results in a CompletableFuture
        return allFutures.thenApply(v -> {
            List<Item> processedItems = new ArrayList<>();
            for (CompletableFuture<Item> future : obtainedFuturesList) {
                try {
                    Item item = future.join();   // getting the result of each individual future
                    if (item != null) {          // skipping the ones that returned null
                        processedItems.add(item);
                    }
                } catch (Exception exception) {
                    System.err.println("Error retrieving result: " + exception.getMessage());
                }
            }
            return processedItems;
        });
    }
}

