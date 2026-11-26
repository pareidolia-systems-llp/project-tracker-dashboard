# Project Instruction import

`Project Instruction` is an optional sheet column. Blank or missing cells are
stored as SQL NULL. The initial import accepts a plain HTTPS URL on the exact
host `drive.google.com` or `docs.google.com`, with no user information and no
port other than the default HTTPS port or 443. The complete trimmed URL,
including query parameters, is retained, up to 2048 characters. Links are not
opened or fetched.

Invalid, overlong, or unsupported values encountered during row mapping become
NULL and produce a fixed warning without the URL or other cell contents.
Synchronization continues. During snapshot replacement an invalid value thus
replaces any previously stored URL for that reporting row with NULL.
Duplicate normalized Project Instruction headers are rejected before snapshot
replacement. Instruction-only spacer rows are not mapped or validated.
Hyperlinked display text, HYPERLINK formula destinations, smart chips, and
multiple URLs in one cell are not supported.

Instructions are stored only on rows already retained by the existing
project parser. An instruction-only spacer does not create a reporting row,
and instructions are not copied to other reporting periods. Normal sheets are
read in a single request to preserve row alignment; the historical Feb2025
read remains A2:Z and does not read columns beyond Z.
