---
title: Tutorials
description: Squery Tutorials
pagination:
  enabled: false
---

# {{ page.title }}

{%

set tutorials = [
  { label: "Setup", url: "/tutorials/setup.html" },
  { label: "Quickstart", url: "/tutorials/quickstart.html" },
  { label: "Generating Code", url: "/tutorials/codegen.html" }
]

%}

{% for tut in tutorials %}- [{{ tut.label }}]({{ tut.url}})
{% endfor %}






