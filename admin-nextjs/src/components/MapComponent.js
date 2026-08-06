'use client';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect } from 'react';

// Fix for default marker icons in Leaflet
const createCustomIcon = (color) => {
  return new L.DivIcon({
    html: `
      <div style="position: relative;">
        <div style="background-color: ${color}; width: 14px; height: 14px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 15px ${color}; z-index: 10;"></div>
        <div style="position: absolute; top: 100%; left: 50%; transform: translateX(-50%); width: 0; height: 0; border-left: 6px solid transparent; border-right: 6px solid transparent; border-top: 8px solid white; margin-top: -2px;"></div>
      </div>
    `,
    className: 'custom-div-icon',
    iconSize: [14, 14],
    iconAnchor: [7, 22],
  });
};

const RecenterMap = ({ lat, lng }) => {
  const map = useMap();
  useEffect(() => {
    if (lat && lng) {
      map.setView([lat, lng], 15, { animate: true });
    }
  }, [lat, lng, map]);
  return null;
};

export default function MapComponent({ partners = [], selectedPartner }) {
  // Center on selected partner or default to Surat
  const center = selectedPartner?.lat ? [selectedPartner.lat, selectedPartner.lng] : [21.1702, 72.8311];

  return (
    <div style={{ height: '100%', width: '100%', background: '#f8fafc' }}>
      <MapContainer
        center={center}
        zoom={13}
        style={{ height: '100%', width: '100%' }}
        zoomControl={false}
        scrollWheelZoom={true}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        />

        {Array.isArray(partners) && partners.map((p) => {
          if (!p.lat || !p.lng) return null;

          const statusColor = p.status === 'online' ? '#10b981' : p.status === 'busy' ? '#4f46e5' : '#94a3b8';

          return (
            <Marker
              key={p.uid}
              position={[p.lat, p.lng]}
              icon={createCustomIcon(statusColor)}
            >
              <Popup offset={[0, -20]}>
                <div className="p-1 min-w-[100px]">
                  <p className="font-black text-slate-800 uppercase text-[10px] tracking-tight mb-1">{p.name}</p>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: statusColor }}></div>
                    <p className="text-[8px] font-bold text-slate-500 uppercase tracking-widest">{p.status}</p>
                  </div>
                </div>
              </Popup>
            </Marker>
          );
        })}

        <RecenterMap lat={selectedPartner?.lat} lng={selectedPartner?.lng} />
      </MapContainer>
    </div>
  );
}
