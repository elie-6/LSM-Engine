# LSM Engine

A **minimal, durable LSM-style storage engine** in Java, implementing the core fundamentals of modern write-optimized databases.

Built from principles to demonstrate the tradeoffs between durability, throughput, and read performance — no external libraries, just OS semantics and proper fsync ordering.

---

## Overview

This engine accepts `put(key, value)` and `get(key)` operations while guaranteeing **crash-safe durability** through a carefully orchestrated sequence:

1. **Write-Ahead Log (WAL)** with batched `fsync` for acknowledged persistence
2. **MemTable** as an in-memory sorted buffer (TreeMap)
3. **SSTable flushes** — sorted, sequential disk writes with atomic WAL truncation
4. **WAL replay** on startup to recover unflushed state
5. **Binary-searchable SSTables** via in-memory sparse indexing

---

## Flow Architecture 
```
    +----------------+
    |   LSMEngine    |
    | (put/get/close)|
    +--------+-------+
             |
             v
    +----------------+
    |      WAL       |  <- Append-only log with batched fsync
    | (write/replay) |
    +--------+-------+
             |
             v
    +----------------+
    |   MemTable     |  <- In-memory sorted map (TreeMap)
    | (threshold N)  |
    +--------+-------+
             |
   flush threshold reached
             v
    +----------------+
    |   SSTable      |  <- Immutable on-disk file
    | (sorted keys,  |     + WAL truncates (reset)
    |in-memory index)|
    +----------------+
```

---

## Tech Stack

**Language**: Java 17 (Temurin)  
**Build**: Maven  

**Core Components**:
- `com.elie.lsm.wal.WAL` — append-only log with batched `fsync` and replay logic
- `com.elie.lsm.memtable.MemTable` — in-memory sorted map (`TreeMap<String, String>`)
- `com.elie.lsm.sstable.SSTable` — immutable on-disk files with indexed binary search
- `com.elie.lsm.engine.LSMEngine` — orchestration layer for put/get/flush/close

**Design Format**: Plain-text SSTable format (`key=value\n`) for transparency and clarity.

---

## Project Structure
```
src/main/java/com/elie/lsm/
  ├─ Main.java                  # manual tests
  ├─ MainCrashWrite.java        # simulate crash (write → exit)
  ├─ MainCrashRead.java         # recovery test (WAL replay)
  ├─ MainBenchmarkWrite.java    # WAL-only throughput
  ├─ MainBenchmarkFlush.java    # full LSM benchmark
  ├─ LSMUtils.java              # utilities
  ├─ engine/LSMEngine.java
  ├─ wal/WAL.java
  ├─ memtable/MemTable.java
  └─ sstable/SSTable.java

data/                           # runtime: wal.log + segment_*.dat (sstable)
```

---

## Quick Start

### Build
```bash
mvn clean package
```

### Run crash simulation
```bash
java -cp target/classes com.elie.lsm.MainCrashWrite
java -cp target/classes com.elie.lsm.MainCrashRead
```

### Run benchmarks
```bash
# WAL-only

java -cp target/classes com.elie.lsm.MainBenchmarkWrite

# Full LSM (with MemTable flushes)
java -cp target/classes com.elie.lsm.MainBenchmarkFlush
```

### Clean data
```bash
rm -rf data/*
```

---

## Benchmarks (M1 MacBook, SSD)

| Configuration | Entries | Time (s) | Throughput (ops/sec) |
|---------------|---------|----------|---------------------|
| WAL-only (no flush) | 100,000 | 1.15 | ~87,000 |
| WAL + MemTable flush | 100,000 | 2.47 | ~40,000 |
| Aggressive flush (MEM=100) | 100,000 | 3.15 | ~31,000 |

**Key observation**: Batched `fsync` and larger MemTable thresholds significantly improve throughput. Flushing to SSTables incurs disk I/O costs but enables sorted, scalable reads.

---

## Durability Guarantees

Every `put` follows this sequence:

1. Append to WAL (append-only)
2. `writer.flush()` — JVM buffer → OS page cache
3. Batched `fos.getFD().sync()` — OS → physical disk
4. Update MemTable (in-memory)

On MemTable flush:

1. Write sorted SSTable to disk
2. `flush()` + `sync()` the SSTable file
3. **Only then**  WAL is truncated, ensuring no acknowledged write can vanish after a crash.

**Crash recovery**: On startup, `wal.replay()` reconstructs the MemTable from any unflushed WAL entries.

This ordering ensures that no acknowledged write can be lost after `close()` returns.

---

## Design Highlights

1. **WAL batching**: `fsync` is expensive. I batch writes and call `FileDescriptor.sync()` every N operations to balance durability and throughput.

2. **MemTable flush threshold**: The in-memory MemTable holds entries until a configurable threshold. Flushing it to SSTables ensures sequential disk writes and sorted keys for fast retrieval.  

3. **SSTable indexing**: On construction, I build an in-memory index of offsets and keys. Lookups use binary search, then `RandomAccessFile.seek(offset)` for direct access — avoiding full scans.

4. **Crash recovery**: On startup, WAL is replayed into MemTable before any reads, ensuring durability even if a crash happens before MemTable flush.  

5. **Sorted key-value storage**: SSTables store keys in order. Combined with MemTable, this guarantees that `get()` returns the latest value efficiently.  

6. **Benchmarking hooks**: Built-in benchmark mains let me measure WAL-only throughput vs full MemTable flush throughput. This shows real-world tradeoffs between speed and durability.  

7. **Configurable durability**: `FSYNC_EVERY` and `MEMTABLE_THRESHOLD` are adjustable to demonstrate different durability-performance tradeoffs.


   



## Known Limitations

- **No compaction**: Multiple SSTables accumulate over time. Compaction would merge them and remove overwritten keys.
- **Plain-text format**: SSTables use `key=value\n` for clarity. A binary format would reduce parsing overhead.
- **Full in-memory index**: For larger datasets, sparse indexing + Bloom filters would reduce memory footprint and disk I/O.

---

## Why This Matters

This project demonstrates:

- **Durability invariants**: Understanding when data is truly persistent (flush vs. fsync)
- **Write amplification tradeoffs**: WAL + MemTable + SSTable = multiple writes per logical operation
- **Read optimization**: Sorted SSTables + indexing enable efficient lookups despite disk storage
- **Crash recovery**: Proper WAL replay guarantees consistency across failures

These are the foundational concepts behind RocksDB, LevelDB, Cassandra, and HBase — implemented here without abstraction to expose the underlying mechanics.

---

