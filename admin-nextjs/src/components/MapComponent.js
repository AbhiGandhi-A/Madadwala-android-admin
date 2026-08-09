'use client';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect } from 'react';

// Elite Marker: Profile Image with High-Contrast Border and Status Dot
const createEliteMarker = (status, imageUrl, isSOS = false) => {
  const statusColor = isSOS ? '#ef4444' : (status === 'online' ? '#10b981' : status === 'busy' ? '#6366f1' : '#94a3b8');
  const img = imageUrl || 'https://via.placeholder.com/40';

  return new L.DivIcon({
    html: `
      <div class="elite-marker-wrapper ${isSOS ? 'sos-blink' : ''}" style="position: relative; width: 50px; height: 50px;">
        <div style="
          width: 48px;
          height: 48px;
          border-radius: 50%;
          border: 4px solid ${isSOS ? '#ef4444' : 'white'};
          box-shadow: 0 4px 15px rgba(0,0,0,0.3);
          overflow: hidden;
          background: #f1f5f9;
          display: flex;
          align-items: center;
          justify-content: center;
        ">
          <img src="${img}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.style.display='none'; this.parentElement.innerHTML='<div style=\\\'font-weight:bold;color:#ccc;font-size:10px;\\\'>IMG</div>'" />
        </div>
        <div style="
          position: absolute;
          bottom: 2px;
          right: 2px;
          width: 14px;
          height: 14px;
          background-color: ${statusColor};
          border: 2px solid white;
          border-radius: 50%;
          z-index: 20;
          box-shadow: 0 0 10px ${statusColor};
        "></div>
        <div style="
          position: absolute;
          top: 100%;
          left: 50%;
          transform: translateX(-50%);
          width: 0;
          height: 0;
          border-left: 10px solid transparent;
          border-right: 10px solid transparent;
          border-top: 12px solid ${isSOS ? '#ef4444' : 'white'};
          margin-top: -6px;
        "></div>
      </div>
    `,
    className: 'custom-leaflet-marker',
    iconSize: [50, 60],
    iconAnchor: [25, 60],
  });
};

const MapController = ({ target }) => {
  const map = useMap();
  useEffect(() => {
    if (target?.lat && target?.lng) {
      map.flyTo([target.lat, target.lng], 16, { animate: true, duration: 1.5 });
    }
  }, [target, map]);

  useEffect(() => {
    // Initial size fix for Leaflet in Next.js
    setTimeout(() => { map.invalidateSize(); }, 500);
  }, [map]);

  return null;
};

export default function MapComponent({ partners = [], selectedPartner, sosProviderUid, distressedUser }) {
  // Default to Surat if no markers
  const defaultPos = [21.1702, 72.8311];

  return (
    <div className="w-full h-full bg-slate-50 relative" style={{ minHeight: '400px' }}>
      <MapContainer
        center={defaultPos}
        zoom={12}
        style={{ height: '100%', width: '100%', zIndex: 1 }}
        zoomControl={false}
      >
        <TileLayer
          url="https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
          attribution='&copy; Google Maps'
        />

        {distressedUser && distressedUser.lat && (
            <Marker
                position={[distressedUser.lat, distressedUser.lng]}
                icon={new L.DivIcon({
                    html: `<div class="sos-blink" style="width: 20px; height: 20px; background: #ef4444; border: 3px solid white; border-radius: 50%; box-shadow: 0 0 15px #ef4444;"></div>`,
                    className: 'custom-leaflet-marker',
                    iconSize: [20, 20],
                    iconAnchor: [10, 10],
                })}
            >
                <Popup offset={[0, -10]}>
                    <div className="text-center font-bold text-red-600 text-[10px]">USER LOCATION</div>
                </Popup>
            </Marker>
        )}

        {Array.isArray(partners) && partners.map((p) => {
          if (!p.lat || !p.lng || isNaN(p.lat) || isNaN(p.lng)) return null;
          const isSOS = p.uid === sosProviderUid;

          return (
            <Marker
              key={p.uid}
              position={[p.lat, p.lng]}
              icon={createEliteMarker(p.status, p.profileImage, isSOS)}
              zIndexOffset={isSOS ? 2000 : (selectedPartner?.uid === p.uid ? 1000 : 0)}
            >
              <Popup offset={[0, -50]}>
                <div className="text-center font-sans">
                  <p className="font-bold text-slate-800 uppercase text-[10px]">{p.name}</p>
                  <p className="text-[9px] text-indigo-500 font-bold uppercase">{isSOS ? '🚨 EMERGENCY' : p.status}</p>
                </div>
              </Popup>
            </Marker>
          );
        })}

        <MapController target={selectedPartner || (distressedUser ? { lat: distressedUser.lat, lng: distressedUser.lng } : null)} />
      </MapContainer>

      {/* CSS fix for marker rendering */}
      <style jsx global>{`
        .custom-leaflet-marker {
          background: transparent !important;
          border: none !important;
        }
        .leaflet-marker-icon {
          will-change: transform;
        }
        @keyframes sos-pulse {
          0% { transform: scale(1); opacity: 1; filter: drop-shadow(0 0 0px #ef4444); }
          50% { transform: scale(1.15); opacity: 0.8; filter: drop-shadow(0 0 15px #ef4444); }
          100% { transform: scale(1); opacity: 1; filter: drop-shadow(0 0 0px #ef4444); }
        }
        .sos-blink {
          animation: sos-pulse 1s infinite ease-in-out;
        }
      `}</style>
    </div>
  );
}
