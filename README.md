# AresAML — Anti-Money Laundering Transaction Analysis Tool

A Java desktop tool that models a network of bank transactions as a **directed graph** and automatically detects two common money-laundering patterns: circular transaction rings and structured ("smurfed") transfers.

Built for the Programming Data Structures and Algorithms (PDSA) module, HND in Software Engineering.

## The Problem

Banks process thousands of transactions per hour. Two laundering techniques are hard to catch manually:
- **Circular flows** — funds routed through several accounts before returning to the sender (A → B → C → A)
- **Structuring / smurfing** — a large sum deliberately broken into many small transfers to slip under reporting thresholds

AresAML surfaces both by treating the transaction network as a graph rather than reviewing transactions one at a time.

## Features

- **Cycle Detection** — Depth-First Search identifies circular laundering rings
- **Smurfing Detection** — a sliding time-window scan flags accounts whose small transfers add up to a suspicious total
- **Risk Score Engine** — a transparent, explainable 0–100 score per account (not a black box)
- **Interactive Dashboard** — live transaction feed + one-click network analysis (Java Swing)

## Data Structure

Directed graph, implemented as an adjacency list: `HashMap<Account, List<Transaction>>`. Chosen because real transaction networks are sparse — this gives O(1) average account lookup and O(V + E) cycle detection, versus the wasted space of an adjacency matrix.

## Tech Stack

Java SE 21 · Java Swing · NetBeans IDE

## How to Run

**Console demo** (shows cycle detection, smurfing detection, and risk scores in the terminal):
```
Run: aresaml.AresAML
```

**GUI Dashboard**:
```
Run: aresaml.AresDashboard
```
In NetBeans: right-click `AresDashboard.java` → Run File.

## Screenshots

_Add a screenshot of the dashboard here after running it._

## Project Structure

```
src/aresaml/
├── Account.java        - represents a bank account (graph node)
├── Transaction.java    - represents a money transfer (graph edge)
├── AMLGraph.java        - the graph + cycle detection + smurfing detection + risk scoring
├── AresAML.java         - console entry point / demo
└── AresDashboard.java   - Swing GUI
```
