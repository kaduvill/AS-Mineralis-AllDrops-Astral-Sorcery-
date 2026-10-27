# AS Mineralis - All Drops

A tiny Minecraft 1.12.2 client-side addon for Astral Sorcery 1.10.27 and JEI/HEI 4.x.

It adds categories for:
- Mineralis ritual
- Containment Chalice reactions

Every concrete item output is indexed for JEI lookup and output/filter helpers.

Aim: QoL for recipe-transfer handlers that can fill an entire filter.

Validation: `gradlew test build` runs registry, probability, JEI rebuild, defensive-copy, output lookup, pagination, and hidden-output rendering checks, then builds the Java 8 mod jar.
