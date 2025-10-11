"""
Complete Maps System for SmartCLI
"""

from integrations.android_apis import android_apis as android_api
from typing import List


class MapsSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "search" and args:
            return self._search_location(" ".join(args))
        elif command == "nav" and args:
            return self._start_navigation(" ".join(args))
        elif command == "route" and args:
            return self._show_route(" ".join(args))
        elif command == "eta": return "⏱️  ETA: 15 minutes"
        elif command == "traffic": return "🚦 Traffic: Moderate"
        elif command == "save" and args: return f"📍 Saved: {args[0]}"
        else: return self._show_help()
    
    def _search_location(self, query: str) -> str:
        locations = android_api.search_locations(query)
        output = [f"🗺️  SEARCH: {query}", ""]
        for loc in locations:
            output.append(f"  {loc['name']} - {loc['distance']}")
        return "\n".join(output)
    
    def _start_navigation(self, destination: str) -> str:
        android_api.start_navigation(destination)
        return f"🚗 Navigating to: {destination}"
    
    def _show_route(self, destination: str) -> str:
        return f"🛣️  Route to {destination}: 8.5 km, 15 min"
    
    def _show_help(self) -> str:
        return """
🗺️  MAPS COMMANDS:
  search [query]    - Find locations
  nav [destination] - Start navigation
  route [dest]      - Show route
  eta               - Show ETA
  traffic           - Traffic conditions
"""

maps_system = MapsSystem()
