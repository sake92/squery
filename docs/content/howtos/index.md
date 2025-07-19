---
title: How Tos
description: Squery How Tos
pagination:
  enabled: false
---

# {{ page.title }}

{%

set howtos = [
  { label: "Queries", url: "/howtos/queries.html" },
  { label: "Updates", url: "/howtos/updates.html" },
  { label: "Interpolation", url: "/howtos/interpolation.html" },
  { label: "Transactions", url: "/howtos/transactions.html" },
  { label: "Custom Types", url: "/howtos/custom_types.html" }
]

%}

{% for howto in howtos %}- [{{ howto.label }}]({{ howto.url}})
{% endfor %}






