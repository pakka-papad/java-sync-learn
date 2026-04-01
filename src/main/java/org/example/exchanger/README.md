## Exchanger Pattern

Implement a simplified Exchanger for two threads to swap data.

Requirements:
- Two threads call exchange() method
- First thread blocks until second arrives
- Threads exchange data and both return
- Support timeout for exchange
- Handle interruption
- Make it work for exactly two parties (not N parties)

Key Concepts: Rendezvous, data exchange, thread coordination, pairing